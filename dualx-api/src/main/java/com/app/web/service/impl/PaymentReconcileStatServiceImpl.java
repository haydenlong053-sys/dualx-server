package com.app.web.service.impl;

import com.app.common.enums.BizStatusEnum;
import com.app.common.enums.ReconcileStatusEnum;
import com.app.common.enums.ReconcileTypeEnum;
import com.app.common.util.BaseUtil;
import com.app.db.entity.PaymentReconcileLog;
import com.app.db.entity.PaymentReconcileStat;
import com.app.db.mapper.PaymentReconcileStatMapper;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IPaymentReconcileLogService;
import com.app.web.service.IPaymentReconcileStatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * 支付对账每日统计表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Service
@Slf4j
public class PaymentReconcileStatServiceImpl extends ServiceImpl<PaymentReconcileStatMapper, PaymentReconcileStat> implements IPaymentReconcileStatService {


    @Resource
    private IPaymentReconcileLogService paymentReconcileLogService;

    @Resource
    private PaymentReconcileStatMapper paymentReconcileStatMapper;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 统计前一天数据
     */
    public void statPreviousDay() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        LocalDateTime startTime = statDate.atStartOfDay();
        LocalDateTime endTime = statDate.plusDays(1).atStartOfDay();

        log.info("开始统计支付对账, statDate={}", statDate);

        // 统计 USDT
        List<Map<String, Object>> usdtResult = paymentReconcileStatMapper.statByToken(startTime, endTime, withdrawContractConfig.getUsdtContract());
        saveOrUpdateStat(statDate, 1, "USDT", usdtResult);

        // 统计 ODIC
        List<Map<String, Object>> odicResult = paymentReconcileStatMapper.statByToken(startTime, endTime, withdrawContractConfig.getOdicContract());
        saveOrUpdateStat(statDate, 2, "ODIC", odicResult);

        // 统计 ODIC
        List<Map<String, Object>> duonResult = paymentReconcileStatMapper.statByToken(startTime, endTime, withdrawContractConfig.getDuonContract());
        saveOrUpdateStat(statDate, 3, "DUON", duonResult);

        log.info("支付对账统计完成, statDate={}", statDate);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReconcileTask() {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("支付对账任务仍在运行中，本次跳过");
            return;
        }
        try {
            executeReconcile();
        } catch (Exception e) {
            log.error("支付订单对账任务异常，事务将回滚", e);
            throw new RuntimeException("支付对账任务执行失败，事务已回滚", e);
        } finally {
            isRunning.set(false);
        }
    }


    // ==================== 核心业务逻辑 ====================

    private void executeReconcile() {
        List<PaymentReconcileLog> pendingList = queryPendingReconcileOrders();

        if (!BaseUtil.Base_HasValue(pendingList)) {
            log.debug("无待对账支付订单");
            return;
        }
        log.info("开始支付订单对账，待处理数量={}", pendingList.size());
        List<PaymentReconcileLog> successList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (PaymentReconcileLog order : pendingList) {
            reconcileSingleOrder(order, now);
            successList.add(order);
        }
        if (!successList.isEmpty()) {
            paymentReconcileLogService.updateBatchById(successList);
            log.info("支付订单对账完成，成功处理数量={}", successList.size());
        }
    }

    private List<PaymentReconcileLog> queryPendingReconcileOrders() {
        LocalDateTime waitThreshold = LocalDateTime.now().minusMinutes(20);
        return paymentReconcileLogService.lambdaQuery()
                .eq(PaymentReconcileLog::getReconcileStatus, ReconcileStatusEnum.PENDING.getCode())
                .and(wrapper -> wrapper
                        .le(PaymentReconcileLog::getUpdateTime, waitThreshold)
                        .or(w -> w.eq(PaymentReconcileLog::getChainExists, 1)
                                .eq(PaymentReconcileLog::getBizExists, 1))
                )
                .orderByAsc(PaymentReconcileLog::getUpdateTime)
                .last("LIMIT " + 500)
                .list();
    }

    private void reconcileSingleOrder(PaymentReconcileLog order, LocalDateTime now) {
        order.setReconcileAmountMatch(0);
        order.setReconcileHashMatch(0);

        if (isBizExists(order) && !isChainExists(order)) {
            handleBizExistsChainNot(order, now);
            return;
        }

        if (!isBizExists(order) && isChainExists(order)) {
            handleChainExistsBizNot(order, now);
            return;
        }

        performDetailedReconcile(order, now);
    }

    // ==================== 对账判断方法 ====================
    private boolean isBizExists(PaymentReconcileLog order) {
        return Integer.valueOf(1).equals(order.getBizExists());
    }

    private boolean isChainExists(PaymentReconcileLog order) {
        return Integer.valueOf(1).equals(order.getChainExists());
    }

    // ==================== 对账结果处理方法 ====================

    private void handleBizExistsChainNot(PaymentReconcileLog order, LocalDateTime now) {
        if (order.getBizStatus() == BizStatusEnum.FAIL.getCode()) {
            markAsSuccess(order, now, "对账成功：业务订单为失败，链上没有记录属于正常情况");
            return;
        }
        markAsFail(order, now, ReconcileTypeEnum.TYPE_BIZ_HAS_CHAIN_NONE.getCode(), "业务系统存在订单，但未发现对应链上支付成功事件，需要人工确认");
    }

    private void handleChainExistsBizNot(PaymentReconcileLog order, LocalDateTime now) {
        markAsFail(order, now, ReconcileTypeEnum.TYPE_CHAIN_HAS_BIZ_NONE.getCode(), "链上存在支付成功事件，但业务系统未同步对应订单，需要人工确认");
    }

    private void performDetailedReconcile(PaymentReconcileLog order, LocalDateTime now) {
        boolean amountMatch = checkAmountMatch(order);
        boolean hashMatch = checkHashMatch(order);
        boolean statusMatch = checkStatusMatch(order);

        order.setReconcileAmountMatch(amountMatch ? 1 : 0);
        order.setReconcileHashMatch(hashMatch ? 1 : 0);
        order.setReconcileStatusMatch(statusMatch ? 1 : 0);

        if (amountMatch && hashMatch && statusMatch) {
            markAsSuccess(order, now, "对账成功：业务订单与链上支付成功事件一致");
        } else {
            String mismatchDetail = buildMismatchDetail(order, amountMatch, hashMatch, statusMatch);
            int mismatchType = determineMismatchType(amountMatch, hashMatch, statusMatch);
            markAsFail(order, now, mismatchType,
                    String.format("对账异常：%s orderId=%s", mismatchDetail, order.getBizOrderNumber()));
        }
    }

    // ==================== 比对方法 ====================

    private boolean checkAmountMatch(PaymentReconcileLog order) {
        return order.getBizTokenAmount() != null
                && order.getChainAmount() != null
                && order.getBizTokenAmount().compareTo(order.getChainAmount()) == 0;
    }

    private boolean checkHashMatch(PaymentReconcileLog order) {
        String bizHash = order.getBizHash();
        String chainHash = order.getChainTxHash();
        return bizHash != null
                && chainHash != null
                && bizHash.trim().equalsIgnoreCase(chainHash.trim());
    }

    private boolean checkStatusMatch(PaymentReconcileLog order) {
        Integer bizStatus = order.getBizStatus();
        return bizStatus != null && bizStatus == BizStatusEnum.SUCCESS.getCode();
    }

    // ==================== 结果构建方法 ====================

    private String buildMismatchDetail(PaymentReconcileLog order,
                                       boolean amountMatch,
                                       boolean hashMatch,
                                       boolean statusMatch) {
        StringBuilder detail = new StringBuilder();

        if (!amountMatch) {
            detail.append(String.format("金额不一致: 链上=%s, 业务=%s; ",
                    order.getChainAmount(), order.getBizTokenAmount()));
        }

        if (!hashMatch) {
            detail.append(String.format("Hash不一致: 业务=%s, 链上=%s; ",
                    order.getBizHash(), order.getChainTxHash()));
        }

        if (!statusMatch) {
            detail.append(String.format("支付状态不一致: 链上事件已成功, 业务状态=%s; ",
                    order.getBizStatus()));
        }

        return detail.toString();
    }

    private int determineMismatchType(boolean amountMatch, boolean hashMatch, boolean statusMatch) {
        if (!amountMatch) return ReconcileTypeEnum.TYPE_AMOUNT_DIFF.getCode();
        if (!hashMatch) return ReconcileTypeEnum.TYPE_HASH_DIFF.getCode();
        if (!statusMatch) return ReconcileTypeEnum.TYPE_STATUS_DIFF.getCode();
        return ReconcileTypeEnum.FULLY_MATCH.getCode();
    }

    private void markAsSuccess(PaymentReconcileLog order, LocalDateTime now, String remark) {
        order.setReconcileStatus(ReconcileStatusEnum.SUCCESS.getCode());
        order.setReconcileType(ReconcileTypeEnum.FULLY_MATCH.getCode());
        order.setReconcileRemark(remark);
        order.setReconcileTime(now);
    }

    private void markAsFail(PaymentReconcileLog order, LocalDateTime now, int type, String remark) {
        order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
        order.setReconcileType(type);
        order.setReconcileRemark(remark);
        order.setReconcileTime(now);
    }

    /**
     * 保存或更新统计
     */
    private void saveOrUpdateStat(LocalDate statDate, Integer coinId, String coinName, List<Map<String, Object>> resultList) {
        // 查询是否已存在
        LambdaQueryWrapper<PaymentReconcileStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentReconcileStat::getStatDate, statDate)
                .eq(PaymentReconcileStat::getCoinId, coinId);
        PaymentReconcileStat exist = paymentReconcileStatMapper.selectOne(wrapper);
        // 构建统计实体
        PaymentReconcileStat stat = new PaymentReconcileStat();
        stat.setStatDate(statDate);
        stat.setCoinId(coinId);
        stat.setCoinName(coinName);
        stat.setUpdateTime(LocalDateTime.now());
        // 如果有统计数据，使用实际值
        if (resultList != null && !resultList.isEmpty()) {
            Map<String, Object> data = resultList.get(0);
            Integer totalCount = ((Number) data.get("totalCount")).intValue();
            stat.setTotalCount(totalCount);
            stat.setTotalAmount((BigDecimal) data.get("totalAmount"));
            stat.setSuccessCount(((Number) data.get("successCount")).intValue());
            stat.setSuccessAmount((BigDecimal) data.get("successAmount"));
            stat.setExceptionCount(((Number) data.get("exceptionCount")).intValue());
            stat.setExceptionAmount((BigDecimal) data.get("exceptionAmount"));
            log.info("有支付数据, coinId={}, statDate={}, total={}", coinId, statDate, totalCount);
        } else {
            // 没有数据，保存默认值0
            stat.setTotalCount(0);
            stat.setTotalAmount(BigDecimal.ZERO);
            stat.setSuccessCount(0);
            stat.setSuccessAmount(BigDecimal.ZERO);
            stat.setExceptionCount(0);
            stat.setExceptionAmount(BigDecimal.ZERO);
            log.info("无支付数据, 保存默认0值, coinId={}, statDate={}", coinId, statDate);
        }
        // 插入或更新
        if (exist != null) {
            stat.setId(exist.getId());
            stat.setCreateTime(exist.getCreateTime());
            this.updateById(stat);
            log.info("更新统计: coinId={}, statDate={}", coinId, statDate);
        } else {
            stat.setCreateTime(LocalDateTime.now());
            this.save(stat);
            log.info("插入统计: coinId={}, statDate={}", coinId, statDate);
        }
    }
}
