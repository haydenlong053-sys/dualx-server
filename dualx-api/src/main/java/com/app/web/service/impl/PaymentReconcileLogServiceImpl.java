package com.app.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.app.common.constants.RedisConstants;
import com.app.common.constants.SysConfigConstants;
import com.app.common.dto.PaymentOrderLog;
import com.app.common.enums.ReconcileStatusEnum;
import com.app.common.enums.SendStatusEnum;
import com.app.common.model.BaseResult;
import com.app.common.util.BaseUtil;
import com.app.common.util.HttpUtils;
import com.app.common.util.RedisUtil;
import com.app.db.entity.PaymentReconcileLog;
import com.app.db.mapper.PaymentReconcileLogMapper;
import com.app.web.config.RabbitMqConfig;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IMultiTokenPaymentService;
import com.app.web.service.IPaymentReconcileLogService;
import com.app.web.service.mq.WithdrawMqProducer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthLog;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PaymentReconcileLogServiceImpl extends ServiceImpl<PaymentReconcileLogMapper, PaymentReconcileLog> implements IPaymentReconcileLogService {

    @Autowired(required = false)
    private WithdrawMqProducer withdrawMqProducer;


    @Resource
    private IMultiTokenPaymentService multiTokenPaymentService;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;



    @Override
    public PaymentReconcileLog getByOrderNumber(String orderNumber) {
        return getOne(new LambdaQueryWrapper<PaymentReconcileLog>()
                .eq(PaymentReconcileLog::getBizOrderNumber, orderNumber)
                .last("limit 1"));
    }

    /**
     * 链上事件保存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaySuccessEvent(EthLog.LogObject logObject, Event PAY_SUCCESS_EVENT) {
        List<String> topics = logObject.getTopics();
        List<Type> dataList = FunctionReturnDecoder.decode(logObject.getData(), PAY_SUCCESS_EVENT.getNonIndexedParameters());

        if (topics == null || topics.size() < 4) {
            throw new RuntimeException("PaySuccess topics 不完整");
        }
        if (dataList == null || dataList.size() < 4) {
            throw new RuntimeException("PaySuccess data 解析失败");
        }
        String txHash = logObject.getTransactionHash().toLowerCase();
        Long logIndex = logObject.getLogIndex().longValue();
        String orderId = (String) dataList.get(0).getValue();
        BigInteger amount = (BigInteger) dataList.get(1).getValue();
        BigInteger timestamp = (BigInteger) dataList.get(2).getValue();
        BigInteger source = (BigInteger) dataList.get(4).getValue();
        // 幂等性检查
        if (existsByTxHashAndLogIndex(txHash, logIndex)) {
            log.info("支付事件已存在, txHash={}", txHash);
            return;
        }
        // 查询或创建对账记录
        PaymentReconcileLog reconcileLog = getByOrderNumber(orderId);
        if (reconcileLog == null) {
            reconcileLog = new PaymentReconcileLog();
            reconcileLog.setCreateTime(LocalDateTime.now());
        }

        // 链上数据已存在则跳过
        if (Integer.valueOf(1).equals(reconcileLog.getChainExists())) {
            log.info("链上数据已存在, orderId={}", orderId);
            return;
        }

        // 构建链上数据
        buildChainData(reconcileLog, topics, txHash, source, orderId, logIndex, amount, timestamp, logObject.getBlockNumber().longValue());

        // 保存或更新
        saveOrUpdate(reconcileLog);
        log.info("链上数据写入成功, orderId={}", orderId);
    }

    /**
     * 获取待同步MQ的事件列表
     */
    @Override
    public List<PaymentReconcileLog> getPendingList(int syncStatus, int batchSize) {
        return list(new LambdaQueryWrapper<PaymentReconcileLog>()
                .eq(PaymentReconcileLog::getSendStatus, syncStatus)
                .eq(PaymentReconcileLog::getChainExists, 1)
                .orderByAsc(PaymentReconcileLog::getId)
                .last("LIMIT " + batchSize));
    }

    /**
     * 处理MQ同步事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processSyncEvent() {
        List<PaymentReconcileLog> pendingList = this.getPendingList(0, 100);
        if (!BaseUtil.Base_HasValue(pendingList)) {
            log.debug("无待同步的支付事件");
            return;
        }
        log.info("发现待同步支付事件, 数量={}", pendingList.size());
        for (PaymentReconcileLog event : pendingList) {
            // 乐观锁更新状态为处理中
            if (!lambdaUpdate()
                    .set(PaymentReconcileLog::getSendStatus, SendStatusEnum.PROCESSING.getCode())
                    .eq(PaymentReconcileLog::getId, event.getId())
                    .eq(PaymentReconcileLog::getSendStatus, SendStatusEnum.PENDING.getCode())
                    .update()) {
                log.info("事件已被其他任务处理, eventId={}", event.getId());
                return;
            }
            // 构建MQ消息
            JSONObject msg = new JSONObject()
                    .fluentPut("orderId", event.getBizOrderNumber())
                    .fluentPut("source", event.getSource())
                    .fluentPut("userAddress", event.getChainUserAddress())
                    .fluentPut("tokenAddress", event.getChainTokenAddress())
                    .fluentPut("amount", event.getChainAmount().stripTrailingZeros().toPlainString())
                    .fluentPut("txHash", event.getChainTxHash())
                    .fluentPut("timestamp", event.getChainTimestamp().getTime() / 1000)
                    .fluentPut("blockNumber", String.valueOf(event.getChainBlockNumber()));
            boolean sendSuccess;
            if (BaseUtil.Base_HasValue(event.getSource()) && event.getSource() == 2) {
                // 发送MQ给美区
                withdrawMqProducer.send(msg.toJSONString(), RabbitMqConfig.USER_PURCHASE_SUCCESS_QUEUE_US);
                sendSuccess = true;
            } else if (BaseUtil.Base_HasValue(event.getSource()) && event.getSource() == 3) {
                sendSuccess = sendIM(withdrawContractConfig.getImUrl() + "asset/wx/chainPay/purchaseSuccess", msg.toJSONString(), event);
            } else if (BaseUtil.Base_HasValue(event.getSource()) && event.getSource() == 4) {
                // 发送MQ给美区
                withdrawMqProducer.send(msg.toJSONString(), RabbitMqConfig.USER_PURCHASE_SUCCESS_QUEUE_PRODUCT);
                sendSuccess = true;
            } else {
                withdrawMqProducer.send(msg.toJSONString(), RabbitMqConfig.USER_PURCHASE_SUCCESS_QUEUE);
                sendSuccess = true;
            }
            if (sendSuccess) {
                // 更新状态为成功
                lambdaUpdate().set(PaymentReconcileLog::getSendStatus, SendStatusEnum.SUCCESS.getCode()).eq(PaymentReconcileLog::getId, event.getId()).update();
                log.info("MQ发送成功, orderId={}", event.getBizOrderNumber());
            }
        }
    }

    private boolean sendIM(String url, String data, PaymentReconcileLog event) {
        try {
            String response = HttpUtils.sendPostJson(url, data, null);
            log.info("同步给IM响应, response{}", response);
            JSONObject object = JSONObject.parseObject(response);
            Integer code = object.getInteger("code");
            if (code == null || code != 0) {
                return true;
            }
        } catch (Exception e) {
            log.error("同步给IM响应：发送异常，orderNo={}", event.getBizOrderNumber(), e);
            return false;
        }
        return false;
    }

    /**
     * 业务订单保存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveBizOrder(PaymentOrderLog bizOrder) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String orderNumber = bizOrder.getOrderNumber();
            PaymentReconcileLog reconcileLog = this.getByOrderNumber(orderNumber);
            boolean isNewRecord = false;
            // 不存在则创建
            if (reconcileLog == null) {
                reconcileLog = new PaymentReconcileLog();
                isNewRecord = true;
            }
            reconcileLog.setBizOrderNumber(orderNumber);
            reconcileLog.setCreateTime(now);
            // 填充业务数据
            reconcileLog.setBizExists(1);
            reconcileLog.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
            reconcileLog.setReconcileRemark("业务数据已写入，等待对账");
            reconcileLog.setBizType(bizOrder.getType());
            reconcileLog.setBizHash(bizOrder.getHash() == null ? null : bizOrder.getHash().toLowerCase());
            reconcileLog.setBizUserId(bizOrder.getUserId());
            reconcileLog.setBizStatus(bizOrder.getStatus() == 5 ? 2 : bizOrder.getStatus());
            reconcileLog.setBizPointAmount(bizOrder.getPointAmount());
            reconcileLog.setBizTokenAmount(bizOrder.getTokenAmount());
            reconcileLog.setBizOrderTime(bizOrder.getOrderTime());
            reconcileLog.setUpdateTime(now);
            // 保存或更新
            if (isNewRecord) {
                this.save(reconcileLog);
                log.debug("新增业务订单成功, orderNumber={}", orderNumber);
            } else {
                this.updateById(reconcileLog);
                log.debug("更新业务订单成功, orderNumber={}", orderNumber);
            }

        } catch (Exception e) {
            log.error("保存业务订单失败, orderNumber={}, error={}", bizOrder.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("保存业务订单失败", e);
        }
    }

    @Override
    public JSONObject getWithdrawalByOrderId(String orderId,String contractAddress) {
        try {
            if (StringUtils.isBlank(orderId)) {
                log.info("订单ID不能为空");
                return null;
            }
            // 频率限制
            String limitKey = RedisConstants.PAYMENT_HASH_QUERY_LIMIT_PREFIX + orderId.trim().toLowerCase();
            boolean canQuery = RedisUtil.setNxEx(limitKey, "1", 3);
            if (!canQuery) {
                return null;
            }
            // 1. 查缓存（已确认的交易）
            if (RedisUtil.hasKey(RedisConstants.PAYMENT_HASH + orderId)) {
                String data = RedisUtil.get(RedisConstants.PAYMENT_HASH + orderId);
                return JSONObject.parseObject(data);
            }
            PaymentReconcileLog event = this.getOne(new LambdaQueryWrapper<PaymentReconcileLog>().eq(PaymentReconcileLog::getBizOrderNumber, orderId));
            if (event == null) {
                JSONObject result = multiTokenPaymentService.getPaymentRecord(orderId, contractAddress);
                if (BaseUtil.Base_HasValue(result)) {
                    return result;
                }
                log.info("{}订单不存在", orderId);
                return null;
            }
            JSONObject result = new JSONObject()
                    .fluentPut("orderId", event.getBizOrderNumber())
                    .fluentPut("source", event.getSource())
                    .fluentPut("userAddress", event.getChainUserAddress())
                    .fluentPut("tokenAddress", event.getChainTokenAddress())
                    .fluentPut("amount", event.getChainAmount().stripTrailingZeros().toPlainString())
                    .fluentPut("txHash", event.getChainTxHash())
                    .fluentPut("timestamp", String.valueOf(event.getChainTimestamp().getTime() / 1000))
                    .fluentPut("blockNumber", String.valueOf(event.getChainBlockNumber()));
            RedisUtil.setEx(RedisConstants.PAYMENT_HASH + orderId, result.toJSONString(), SysConfigConstants.REDIS_ONE_DAY_SECONDS);
            return result;
        } catch (Exception e) {
            log.error("查询订单异常, orderId: {}", orderId);
            return null;
        }
    }

    @Override
    public BaseResult<?> getTokenPrice(BigDecimal amount, Integer status,String withdrawTokenAddress) {
        if (withdrawTokenAddress == null || withdrawTokenAddress.isEmpty()) {
            return BaseResult.error("代币地址不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BaseResult.error("数量必须大于0");
        }
        if (status == null || (status != 1 && status != 2)) {
            return BaseResult.error("status参数错误，1=ODIC转USDT，2=USDT转ODIC");
        }
        String cacheKey = RedisConstants.TOKEN_PRICE_CACHE_PREFIX + withdrawTokenAddress.toLowerCase() + ":rate";
        String dealCacheKey = RedisConstants.PRICE_CACHE_PREFIX_DEAL + withdrawTokenAddress.toLowerCase() + ":rate";
        try {
            BigDecimal rate = null;
            String dealCache = RedisUtil.get(dealCacheKey);
            if (dealCache != null) {
                String cachedRate = RedisUtil.get(cacheKey);
                if (cachedRate != null) {
                    rate = new BigDecimal(cachedRate);
                }
            }
            if (rate == null) {
                rate = multiTokenPaymentService.getTokenPrice(withdrawTokenAddress);
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    String cachedRate = RedisUtil.get(cacheKey);
                    if (cachedRate != null) {
                        log.warn("链上查询失败，返回缓存汇率, tokenAddress={}, rate={}", withdrawTokenAddress, cachedRate);
                        rate = new BigDecimal(cachedRate);
                    } else {
                        return BaseResult.error("查询代币价格失败");
                    }
                } else {
                    String rateStr = rate.stripTrailingZeros().toPlainString();
                    RedisUtil.set(cacheKey, rateStr);
                    RedisUtil.setEx(dealCacheKey, "1", 5);
                    log.info("代币汇率已更新, tokenAddress={}, rate={}, 下次刷新时间{}秒后",
                            withdrawTokenAddress, rateStr, 5);
                }
            }
            BigDecimal result;
            if (status == 1) {
                // ODIC -> USDT
                result = amount.multiply(rate);
            } else {
                // USDT -> ODIC
                result = amount.divide(rate, 18, RoundingMode.DOWN);
            }
            String resultStr = result.stripTrailingZeros().toPlainString();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("price", resultStr);
            return BaseResult.success("成功", jsonObject);
        } catch (Exception e) {
            log.error("查询代币兑换价格异常, tokenAddress={}, status={}, amount={}",
                    withdrawTokenAddress, status, amount, e);
            return BaseResult.error("查询代币价格失败: " + e.getMessage());
        }
    }

    @Override
    public void processMessage(String body) {
        PaymentReconcileLog source = JSONObject.parseObject(body, PaymentReconcileLog.class);
        // 查询是否存在
        PaymentReconcileLog existing = this.getByOrderNumber(source.getBizOrderNumber());
        if (existing == null) {
            // 不存在 → 新增
            this.save(source);
            log.info("新增支付记录，订单号: {}", source.getBizOrderNumber());
        } else {
            baseMapper.updateByOrderNumberSelective(source);
            log.info("更新支付记录，订单号: {}", source.getBizOrderNumber());
        }
    }


    // ==================== 私有方法 ====================

    /**
     * 检查链上事件是否已存在
     */
    private boolean existsByTxHashAndLogIndex(String txHash, Long logIndex) {
        return count(new LambdaQueryWrapper<PaymentReconcileLog>()
                .eq(PaymentReconcileLog::getChainTxHash, txHash)
                .eq(PaymentReconcileLog::getChainLogIndex, logIndex)) > 0;
    }

    /**
     * 构建链上数据
     */
    private void buildChainData(PaymentReconcileLog log, List<String> topics, String txHash, BigInteger source, String orderId,
                                Long logIndex, BigInteger amount, BigInteger timestamp, Long blockNumber) {
        log.setChainExists(1);
        log.setBizOrderNumber(orderId);
        log.setChainUserAddress(topicToAddress(topics.get(1)).toLowerCase());
        log.setChainTokenAddress(topicToAddress(topics.get(2)).toLowerCase());
        log.setChainReceiver(topicToAddress(topics.get(3)).toLowerCase());
        log.setSource(source.intValue());
        log.setChainAmount(new BigDecimal(amount)
                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.DOWN)
                .stripTrailingZeros());
        log.setChainTxHash(txHash);
        log.setChainBlockNumber(blockNumber);
        log.setChainLogIndex(logIndex);
        log.setChainTimestamp(new Date(timestamp.longValue() * 1000));
        log.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
        log.setReconcileRemark("链上数据已写入，等待对账");
        log.setUpdateTime(LocalDateTime.now());
    }

    /**
     * topic 转地址
     */
    private String topicToAddress(String topic) {
        return "0x" + topic.substring(topic.length() - 40).toLowerCase();
    }
}