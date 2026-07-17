package com.app.web.service.impl;

import com.app.common.constants.SysConfigConstants;
import com.app.common.dto.OrderClassificationResult;
import com.app.common.enums.CoinEnum;
import com.app.common.enums.OrderTypeEnum;
import com.app.common.enums.WithdrawalStatusEnum;
import com.app.common.util.BaseUtil;
import com.app.common.util.RedisUtil;
import com.app.db.entity.BscWithdrawalLog;
import com.app.db.entity.WithdrawReconcileLog;
import com.app.db.mapper.BscWithdrawalLogMapper;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IAccessControlService;
import com.app.web.service.IBscWithdrawalLogService;
import com.app.web.service.IWithdrawReconcileLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-22 15:59
 */
@Slf4j
@Service
public class BscWithdrawalLogServiceImpl extends ServiceImpl<BscWithdrawalLogMapper, BscWithdrawalLog> implements IBscWithdrawalLogService {

    @Resource
    private IAccessControlService accessControlService;
    @Resource
    private WithdrawContractConfig withdrawContractConfig;
    @Resource
    private IWithdrawReconcileLogService withdrawReconcileLogService;


    private final AtomicBoolean isRunning = new AtomicBoolean(false);


    @Override
    public Map<String, String> getWithdrawalStatusByOrderId(String orderId) {
        log.info("查询提现记录状态，订单号: {}", orderId);
        Map<String, String> result = new HashMap<>();
        // 优先从Redis缓存查询
        if (RedisUtil.hasKey(orderId + "_status") && RedisUtil.hasKey(orderId + "_hash")) {
            result.put("hash", RedisUtil.get(orderId + "_hash"));
            result.put("status", RedisUtil.get(orderId + "_status"));
            return result;
        }
        // 从数据库查询
        LambdaQueryWrapper<WithdrawReconcileLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WithdrawReconcileLog::getBizOrderNumber, orderId).last("limit 1");
        WithdrawReconcileLog log = withdrawReconcileLogService.getOne(queryWrapper);
        // 未查询到记录
        if (log == null) {
            result.put("hash", null);
            result.put("status", "1");
            return result;
        }
        // 构建返回结果
        String status = BaseUtil.Base_HasValue(log.getChainTxHash()) ? "2" : "1";
        result.put("hash", log.getChainTxHash());
        result.put("status", status);
        return result;
    }

    @Override
    public void classifyOrderByRequiredSignatures() {
        log.debug("开始执行订单类型判断任务");
        if (!isRunning.compareAndSet(false, true)) {
            log.info("订单类型判断任务仍在执行中，本次跳过");
            return;
        }
        try {
            List<BscWithdrawalLog> unclassifiedOrders = queryUnclassifiedOrders();
            if (unclassifiedOrders.isEmpty()) {
                log.debug("无待分类订单");
                return;
            }
            log.info("发现 {} 笔待分类订单", unclassifiedOrders.size());
            Map<Integer, List<BscWithdrawalLog>> ordersByCoin = groupByCoinId(unclassifiedOrders);
            int largeCount = 0;
            int smallCount = 0;
            for (Map.Entry<Integer, List<BscWithdrawalLog>> entry : ordersByCoin.entrySet()) {
                Integer coinId = entry.getKey();
                List<BscWithdrawalLog> orders = entry.getValue();
                String contractAddress = getContractAddressByCoinId(coinId);
                if (contractAddress == null) {
                    log.warn("不支持的币种: coinId={}", coinId);
                    continue;
                }
                log.info("处理币种: coinId={}, 订单数={}", coinId, orders.size());
                for (BscWithdrawalLog order : orders) {
                    OrderClassificationResult result = classifyOrder(order, contractAddress);
                    if (result.isSuccess()) {
                        if (result.isLargeOrder()) {
                            largeCount++;
                        } else {
                            smallCount++;
                        }
                    }
                }
            }
            log.info("订单类型判断任务完成, 大额订单数={}, 小额订单数={}", largeCount, smallCount);
        } catch (Exception e) {
            log.error("订单类型判断任务执行异常", e);
        } finally {
            isRunning.set(false);
        }
    }

    @Override
    public List<BscWithdrawalLog> getDeadlineOrder(LocalDateTime timeoutTime, int limit) {
        return this.lambdaQuery()
                .in(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_PROCESSING.getCode(),
                        WithdrawalStatusEnum.SYSTEM_ERROR.getCode(),
                        WithdrawalStatusEnum.STATUS_CHAIN_CONFIRMING.getCode())
                .lt(BscWithdrawalLog::getUpdateTime, timeoutTime)
                .last("limit " + limit)
                .list();
    }

    @Override
    public boolean markStatus(Integer id, WithdrawalStatusEnum statusEnum, String remark) {
        return this.lambdaUpdate()
                .set(BscWithdrawalLog::getStatus, statusEnum.getCode())
                .set(BscWithdrawalLog::getRemark, remark)
                .set(BscWithdrawalLog::getUpdateTime, LocalDateTime.now())
                .eq(BscWithdrawalLog::getId, id)
                .update();
    }

    @Override
    public String getContractAddress(BscWithdrawalLog order) {
        Integer coinId = order.getCoinId();
        Integer originType = order.getOriginType();

        // 默认值处理
        int type = originType != null ? originType : 0;
        int coin = coinId != null ? coinId : 0;

        // 构建组合 key
        String key = type + "_" + coin;

        // 使用 Map 存储所有组合对应的地址
        Map<String, String> addressMap = new HashMap<>();

        // 非 US 区域 (type != 4)
        addressMap.put("1_1", withdrawContractConfig.getContractWithdrawUsdt());    // 非US + BTC(1)
        addressMap.put("1_2", withdrawContractConfig.getContractWithdrawOdic());   // 非US + 其他币(非1)

        addressMap.put("2_1", withdrawContractConfig.getContractWithdrawUsdt());    // 非US + BTC(1)
        addressMap.put("2_2", withdrawContractConfig.getContractWithdrawOdic());   // 非US + 其他币(非1)

        addressMap.put("3_1", withdrawContractConfig.getContractWithdrawUsdt());    // 非US + BTC(1)
        addressMap.put("3_2", withdrawContractConfig.getContractWithdrawOdic());   // 非US + 其他币(非1)
        // US 区域 (type = 4)
        addressMap.put("4_1", withdrawContractConfig.getUsContractWithdrawUsdt());   // US + BTC(1)
        addressMap.put("4_2", withdrawContractConfig.getUsContractWithdrawOdic()); // US + 其他币(非1)

        addressMap.put("5_1", withdrawContractConfig.getProductContractWithdrawUsdt());   //暂时未定的系统
        addressMap.put("5_2", withdrawContractConfig.getProductContractWithdrawOdic()); // 暂时未定的系统


        // 如果 key 不存在，返回默认值或抛出异常
        return addressMap.getOrDefault(key, withdrawContractConfig.getContractWithdrawOdic());
    }

    private List<BscWithdrawalLog> queryUnclassifiedOrders() {
        return this.lambdaQuery()
                .eq(BscWithdrawalLog::getIsLargeAmount, OrderTypeEnum.UNKNOWN.getCode())
                .orderByAsc(BscWithdrawalLog::getId)
                .last("limit " + 200)
                .list();
    }

    private Map<Integer, List<BscWithdrawalLog>> groupByCoinId(List<BscWithdrawalLog> orders) {
        return orders.stream().collect(Collectors.groupingBy(BscWithdrawalLog::getCoinId));
    }

    private String getContractAddressByCoinId(Integer coinId) {
        if (coinId.equals(CoinEnum.USDT.getCoinId())) {
            return withdrawContractConfig.getContractWithdrawUsdt();
        } else if (coinId.equals(CoinEnum.OIDC.getCoinId())) {
            return withdrawContractConfig.getContractWithdrawOdic();
        }
        return null;
    }

    private OrderClassificationResult classifyOrder(BscWithdrawalLog order, String contractAddress) {
        try {
            BigInteger chainAmount = convertToChainAmount(order);
            if (chainAmount == null) {
                log.warn("金额转换失败，跳过订单, orderId={}", order.getId());
                return OrderClassificationResult.fail();
            }

            BigInteger requiredSignatures = accessControlService.requiredSignaturesForAmount(chainAmount, contractAddress);
            if (requiredSignatures == null) {
                log.warn("获取签名数量失败，跳过订单, orderId={}", order.getId());
                return OrderClassificationResult.fail();
            }
            boolean isLargeOrder = requiredSignatures.compareTo(BigInteger.valueOf(SysConfigConstants.SIGNATURE_THRESHOLD)) > 0;
            int orderType = isLargeOrder ? OrderTypeEnum.LARGE.getCode() : OrderTypeEnum.SMALL.getCode();
            boolean updated = this.lambdaUpdate()
                    .set(BscWithdrawalLog::getIsLargeAmount, orderType)
                    .set(BscWithdrawalLog::getUpdateTime, LocalDateTime.now())
                    .eq(BscWithdrawalLog::getId, order.getId())
                    .eq(BscWithdrawalLog::getIsLargeAmount, OrderTypeEnum.UNKNOWN.getCode())
                    .update();
            if (updated) {
                log.debug("订单标记成功: orderId={}, type={}", order.getId(), isLargeOrder ? "大额" : "小额");
                return OrderClassificationResult.success(isLargeOrder);
            } else {
                return OrderClassificationResult.fail();
            }
        } catch (Exception e) {
            log.error("判断订单类型失败, orderId={}", order.getId(), e);
            return OrderClassificationResult.fail();
        }
    }

    public BigInteger convertToChainAmount(BscWithdrawalLog order) {
        try {
            int decimals = getDecimalsByCoinId(order.getCoinId());
            return order.getAmount().multiply(BigDecimal.TEN.pow(decimals)).toBigIntegerExact();
        } catch (Exception e) {
            log.error("金额转换失败, orderId={}, amount={}, coinId={}",
                    order.getId(), order.getAmount(), order.getCoinId(), e);
            return null;
        }
    }

    private int getDecimalsByCoinId(Integer coinId) {
        if (coinId == null) {
            throw new IllegalArgumentException("coinId不能为空");
        }
        if (coinId == 1 || coinId == 2) {
            return 18;
        }
        throw new IllegalArgumentException("未知coinId: " + coinId);
    }

}
