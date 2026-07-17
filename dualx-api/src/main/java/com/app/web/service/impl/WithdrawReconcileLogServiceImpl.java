package com.app.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.app.common.constants.SysConfigConstants;
import com.app.common.dto.EventData;
import com.app.common.dto.WithdrawOrderMessage;
import com.app.common.enums.BizStatusEnum;
import com.app.common.enums.ReconcileStatusEnum;
import com.app.common.enums.ReconcileTypeEnum;
import com.app.common.util.BaseUtil;
import com.app.common.util.RedisUtil;
import com.app.db.entity.BscWithdrawalLog;
import com.app.db.entity.WithdrawReconcileLog;
import com.app.db.mapper.WithdrawReconcileLogMapper;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IBscWithdrawalLogService;
import com.app.web.service.IWithdrawContractKmsService;
import com.app.web.service.IWithdrawReconcileLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthLog;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * BSC提现对账记录表 服务实现类
 */
@Service
@Slf4j
public class WithdrawReconcileLogServiceImpl extends ServiceImpl<WithdrawReconcileLogMapper, WithdrawReconcileLog> implements IWithdrawReconcileLogService {

    @Resource
    private IBscWithdrawalLogService bscWithdrawalLogService;

    @Resource
    private IWithdrawContractKmsService withdrawContractKmsService;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBizOrder(WithdrawOrderMessage message) {
        String orderNumber = message.getOrderNumber();
        WithdrawReconcileLog existLog = this.lambdaQuery().eq(WithdrawReconcileLog::getBizOrderNumber, orderNumber).one();

        if (existLog != null) {
            updateExistBizOrder(existLog, message);
            log.info("更新提现业务订单成功, orderNumber={}", orderNumber);
        } else {
            createNewBizOrder(message);
            log.info("新增提现业务订单成功, orderNumber={}", orderNumber);
        }
    }

    /**
     * 更新已存在的业务订单
     */
    private void updateExistBizOrder(WithdrawReconcileLog existLog, WithdrawOrderMessage message) {
        existLog.setBizOrderExists(1);
        existLog.setBizOrderNumber(message.getOrderNumber());
        existLog.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
        existLog.setReconcileRemark("业务数据已写入，等待对账");
        existLog.setBizOriginType(message.getOriginType());
        existLog.setBizUserId(message.getUserId());
        existLog.setBizCoinId(message.getCoinId());
        existLog.setBizToAddress(message.getToAddress());
        existLog.setBizAmount(message.getAmount());
        existLog.setBizRedemption(message.getRedemption());
        existLog.setBizHash(message.getHash());
        existLog.setBizStatus(message.getStatus());
        existLog.setUpdateTime(LocalDateTime.now());
        this.updateById(existLog);
    }

    /**
     * 创建新的业务订单
     */
    private void createNewBizOrder(WithdrawOrderMessage message) {
        WithdrawReconcileLog newLog = new WithdrawReconcileLog();
        newLog.setBizOrderNumber(message.getOrderNumber());
        newLog.setBizOrderExists(1);
        newLog.setBizOriginType(message.getOriginType());
        newLog.setBizUserId(message.getUserId());
        newLog.setBizCoinId(message.getCoinId());
        newLog.setBizToAddress(message.getToAddress());
        newLog.setBizAmount(message.getAmount());
        newLog.setBizRedemption(message.getRedemption());
        newLog.setBizHash(message.getHash());
        newLog.setBizStatus(message.getStatus());
        newLog.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
        newLog.setReconcileRemark("业务数据已写入，等待对账");
        newLog.setCreateTime(LocalDateTime.now());
        newLog.setUpdateTime(LocalDateTime.now());
        this.save(newLog);
    }

    // ==================== 链上事件处理方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithdrawExecutedEvent(EthLog.LogObject logObject, String contractAddress, String contractType, Event withdrawExecutedEvent) throws Exception {
        String txHash = logObject.getTransactionHash().toLowerCase();
        Long logIndex = logObject.getLogIndex().longValue();

        // 步骤1：幂等性检查
        if (isEventProcessed(txHash, logIndex)) {
            log.info("提现事件已处理过，跳过, txHash={}, logIndex={}", txHash, logIndex);
            return;
        }

        // 步骤2：解析链上事件数据
        EventData eventData = parseWithdrawEvent(logObject, withdrawExecutedEvent);

        // 步骤3：构建对账记录
        WithdrawReconcileLog reconcileLog = buildReconcileLogFromEvent(eventData, logObject, contractAddress, contractType);

        // 步骤4：查询本地订单
        BscWithdrawalLog localOrder = queryLocalOrder(eventData.getOrderId());

        // 步骤5：执行对账逻辑
        performReconciliationWithEvent(reconcileLog, localOrder, eventData, contractAddress);

        // 步骤6：保存对账记录
        saveReconcileLog(reconcileLog, eventData.getOrderId(), txHash, logIndex);
    }

    @Override
    public List<WithdrawReconcileLog> queryPendingReconcileOrders() {
        LocalDateTime waitThreshold = LocalDateTime.now().minusMinutes(20);
        return this.lambdaQuery()
                .eq(WithdrawReconcileLog::getReconcileStatus, ReconcileStatusEnum.PENDING.getCode())
                .and(wrapper -> wrapper.le(WithdrawReconcileLog::getUpdateTime, waitThreshold)
                        .or(w -> w.eq(WithdrawReconcileLog::getChainOrderExists, 1)
                                .eq(WithdrawReconcileLog::getBizOrderExists, 1)))
                .orderByAsc(WithdrawReconcileLog::getUpdateTime)
                .last("LIMIT 500").list();
    }

    @Override
    public void processMessage(String body) {
        WithdrawReconcileLog source = JSONObject.parseObject(body, WithdrawReconcileLog.class);
        String orderId=source.getBizOrderNumber();
        RedisUtil.setEx(orderId + "_status", "2", 24 * 60 * 60L);
        RedisUtil.setEx(orderId + "_hash", source.getChainTxHash(), 24 * 60 * 60L);
        // 查询是否存在
        WithdrawReconcileLog existing = this.lambdaQuery().eq(WithdrawReconcileLog::getBizOrderNumber, source.getBizOrderNumber()).one();
        if (existing == null) {
            // 不存在 → 新增
            this.save(source);
            log.info("新增提现记录，订单号: {}", source.getBizOrderNumber());
        } else {
            // 🔥 存在 → 只更新非 null 字段
            baseMapper.updateByOrderNumberSelective(source);
            log.info("更新提现记录，订单号: {}", source.getBizOrderNumber());
        }
    }

    /**
     * 幂等性检查
     */
    private boolean isEventProcessed(String txHash, Long logIndex) {
        return this.lambdaQuery()
                .eq(WithdrawReconcileLog::getChainTxHash, txHash)
                .eq(WithdrawReconcileLog::getChainLogIndex, logIndex)
                .count() > 0;
    }

    /**
     * 解析提现事件
     */
    private EventData parseWithdrawEvent(EthLog.LogObject logObject, Event withdrawExecutedEvent) {
        List<String> topics = logObject.getTopics();
        if (topics == null || topics.size() < SysConfigConstants.MIN_TOPICS_SIZE) {
            throw new RuntimeException("WithdrawExecuted topics 不完整, size=" + (topics == null ? 0 : topics.size()));
        }

        BigInteger orderId = new BigInteger(topics.get(1).substring(2), 16);
        String userAddress = "0x" + topics.get(2).substring(topics.get(2).length() - 40).toLowerCase();

        List<Type> dataList = FunctionReturnDecoder.decode(logObject.getData(), withdrawExecutedEvent.getNonIndexedParameters());
        if (dataList == null || dataList.size() < SysConfigConstants.MIN_DATA_SIZE) {
            throw new RuntimeException("WithdrawExecuted data 解析失败, size=" + (dataList == null ? 0 : dataList.size()));
        }

        BigInteger amount = (BigInteger) dataList.get(0).getValue();
        BigInteger redemption = (BigInteger) dataList.get(1).getValue();
        String executor = ((Address) dataList.get(3)).getValue();
        BigInteger timestamp = (BigInteger) dataList.get(4).getValue();

        BigDecimal humanAmount = new BigDecimal(amount)
                .divide(BigDecimal.TEN.pow(SysConfigConstants.TOKEN_DECIMALS), SysConfigConstants.TOKEN_DECIMALS, RoundingMode.DOWN)
                .stripTrailingZeros();

        return EventData.builder()
                .orderId(orderId.toString())
                .userAddress(userAddress)
                .humanAmount(humanAmount)
                .redemption(redemption.intValue())
                .executor(executor.toLowerCase())
                .timestamp(new Date(timestamp.longValue() * 1000L))
                .build();
    }

    /**
     * 从事件构建对账记录
     */
    private WithdrawReconcileLog buildReconcileLogFromEvent(EventData eventData, EthLog.LogObject logObject, String contractAddress, String contractType) {
        WithdrawReconcileLog chainLog = new WithdrawReconcileLog();
        chainLog.setChainOrderExists(1);
        chainLog.setBizOrderNumber(eventData.getOrderId());
        chainLog.setChainUserAddress(eventData.getUserAddress());
        chainLog.setChainAmount(eventData.getHumanAmount());
        chainLog.setChainRedemption(eventData.getRedemption());
        chainLog.setChainExecutor(eventData.getExecutor());
        chainLog.setChainTxHash(logObject.getTransactionHash().toLowerCase());
        chainLog.setChainBlockNumber(logObject.getBlockNumber().longValue());
        chainLog.setChainLogIndex(logObject.getLogIndex().longValue());
        chainLog.setChainTimestamp(eventData.getTimestamp());
        chainLog.setChainContractAddress(contractAddress.toLowerCase());
        chainLog.setChainContractType(contractType);
        chainLog.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
        chainLog.setReconcileTime(LocalDateTime.now());
        return chainLog;
    }

    /**
     * 查询本地订单
     */
    private BscWithdrawalLog queryLocalOrder(String orderId) {
        LambdaQueryWrapper<BscWithdrawalLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BscWithdrawalLog::getOrderNumber, orderId);
        queryWrapper.last("LIMIT 1");
        return bscWithdrawalLogService.getOne(queryWrapper, false);
    }

    /**
     * 执行事件对账逻辑
     */
    private void performReconciliationWithEvent(WithdrawReconcileLog reconcileLog, BscWithdrawalLog localOrder,
                                                EventData eventData, String contractAddress) throws Exception {
        if (!BaseUtil.Base_HasValue(localOrder)) {
            // 情况1：本地订单不存在
            handleEventWithoutLocalOrder(reconcileLog, eventData, contractAddress);
        } else {
            // 情况2：本地订单存在，进行比对
            handleEventWithLocalOrder(reconcileLog, localOrder, eventData, contractAddress);
        }
    }

    /**
     * 处理链上有事件但本地无订单的情况
     */
    private void handleEventWithoutLocalOrder(WithdrawReconcileLog reconcileLog, EventData eventData, String contractAddress) throws Exception {
        reconcileLog.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
        reconcileLog.setReconcileType(ReconcileTypeEnum.TYPE_CHAIN_HAS_BIZ_NONE.getCode());
        reconcileLog.setReconcileRemark(String.format("对账异常：链上事件存在，但本地无对应提现订单。orderId=%s, userAddress=%s, amount=%s",
                eventData.getOrderId(), eventData.getUserAddress(), eventData.getHumanAmount()));

    }

    /**
     * 处理链上事件且本地订单也存在的情况
     */
    private void handleEventWithLocalOrder(WithdrawReconcileLog reconcileLog, BscWithdrawalLog localOrder,
                                           EventData eventData, String contractAddress) throws Exception {
        boolean userMatch = StringUtils.equalsIgnoreCase(eventData.getUserAddress(), localOrder.getToAddress());
        boolean amountMatch = eventData.getHumanAmount().compareTo(localOrder.getAmount()) == 0;
        boolean redemptionMatch = eventData.getRedemption().intValue() == localOrder.getRedemption();

        reconcileLog.setReconcileUserMatch(userMatch ? 1 : 0);
        reconcileLog.setReconcileAmountMatch(amountMatch ? 1 : 0);
        reconcileLog.setReconcileRedemptionMatch(redemptionMatch ? 1 : 0);

        if (userMatch && amountMatch && redemptionMatch) {
            // 对账成功，更新本地订单状态
            reconcileLog.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
            reconcileLog.setReconcileType(ReconcileTypeEnum.FULLY_MATCH.getCode());
            reconcileLog.setReconcileRemark("出账对账成功，待业务二次确认。超20分钟未确认则对账失败");

            localOrder.setStatus(BizStatusEnum.SUCCESS.getCode());
            localOrder.setHash(reconcileLog.getChainTxHash());
            localOrder.setUpdateTime(LocalDateTime.now());
            bscWithdrawalLogService.updateById(localOrder);
        } else {
            // 对账失败
            reconcileLog.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
            if (!userMatch) {
                reconcileLog.setReconcileType(ReconcileTypeEnum.TYPE_USER_MISMATCH.getCode());
            } else if (!amountMatch) {
                reconcileLog.setReconcileType(ReconcileTypeEnum.TYPE_AMOUNT_DIFF.getCode());
            } else {
                reconcileLog.setReconcileType(ReconcileTypeEnum.TYPE_REDEMPTION_MISMATCH.getCode());
            }
            reconcileLog.setReconcileRemark(String.format("对账异常：本地订单存在，但链上数据与本地不一致。orderId=%s, userMatch=%s, amountMatch=%s, redemptionMatch=%s, 链上金额=%s, 本地金额=%s, 链上地址=%s, 本地地址=%s",
                    eventData.getOrderId(), userMatch, amountMatch, redemptionMatch,
                    eventData.getHumanAmount().toPlainString(),
                    localOrder.getAmount() != null ? localOrder.getAmount().toPlainString() : "null",
                    eventData.getUserAddress(), localOrder.getToAddress()));
        }
    }

    /**
     * 保存对账记录（更新或新增）
     */
    private void saveReconcileLog(WithdrawReconcileLog reconcileLog, String orderId, String txHash, Long logIndex) {
        WithdrawReconcileLog existLog = this.lambdaQuery().eq(WithdrawReconcileLog::getBizOrderNumber, orderId).one();

        if (BaseUtil.Base_HasValue(existLog)) {
            // 更新已存在的记录
            existLog.setChainOrderExists(1);
            existLog.setBizOrderNumber(reconcileLog.getBizOrderNumber());
            existLog.setChainUserAddress(reconcileLog.getChainUserAddress());
            existLog.setChainAmount(reconcileLog.getChainAmount());
            existLog.setChainRedemption(reconcileLog.getChainRedemption());
            existLog.setChainExecutor(reconcileLog.getChainExecutor());
            existLog.setChainTxHash(reconcileLog.getChainTxHash());
            existLog.setChainBlockNumber(reconcileLog.getChainBlockNumber());
            existLog.setChainLogIndex(reconcileLog.getChainLogIndex());
            existLog.setChainTimestamp(reconcileLog.getChainTimestamp());
            existLog.setChainContractAddress(reconcileLog.getChainContractAddress());
            existLog.setChainContractType(reconcileLog.getChainContractType());
            existLog.setReconcileStatus(reconcileLog.getReconcileStatus());
            existLog.setReconcileType(reconcileLog.getReconcileType());
            existLog.setReconcileRemark(reconcileLog.getReconcileRemark());
            existLog.setReconcileTime(LocalDateTime.now());
            this.updateById(existLog);
        } else {
            // 新增记录
            this.save(reconcileLog);
        }

        log.debug("对账记录保存成功, orderId={}, txHash={}, logIndex={}, status={}",
                orderId, txHash, logIndex, reconcileLog.getReconcileStatus());
    }
}