package com.app.web.service.impl;


import com.app.common.dto.CompareResult;
import com.app.common.enums.BizStatusEnum;
import com.app.common.enums.ReconcileStatusEnum;
import com.app.common.enums.ReconcileTypeEnum;
import com.app.common.util.BaseUtil;
import com.app.db.entity.WithdrawReconcileLog;
import com.app.web.service.IWithdrawReconcileLogService;
import com.app.web.service.IWithdrawReconcileService;
import com.app.web.service.IWithdrawReconcileStatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 提现定时任务实现类
 * <p>
 * 统一管理提现相关的定时任务
 */
@Slf4j
@Component
public class WithdrawReconcileServiceImpl implements IWithdrawReconcileService {

    @Resource
    private IWithdrawReconcileStatService withdrawReconcileStatService;

    @Resource
    private IWithdrawReconcileLogService withdrawReconcileLogService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void executeDailyStat() {
        log.info("提现统计定时任务开始");
        try {
            withdrawReconcileStatService.statPreviousDay();
            log.info("提现统计定时任务完成");
        } catch (Exception e) {
            log.error("提现统计定时任务失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReconcileTask() {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("提现对账任务仍在运行中，本次跳过");
            return;
        }
        try {
            List<WithdrawReconcileLog> pendingList = withdrawReconcileLogService.queryPendingReconcileOrders();
            if (!BaseUtil.Base_HasValue(pendingList)) {
                log.debug("无待对账提现记录");
                return;
            }
            log.info("开始提现订单对账，待处理数量={}", pendingList.size());

            List<WithdrawReconcileLog> successList = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (WithdrawReconcileLog order : pendingList) {
                processSingleReconcileOrder(order, now);
                successList.add(order);
            }
            if (!successList.isEmpty()) {
                withdrawReconcileLogService.updateBatchById(successList);
                log.info("提现订单对账完成，成功处理数量={}", successList.size());
            }
        } catch (Exception e) {
            log.error("提现订单对账任务异常，事务将回滚", e);
            throw new RuntimeException("提现对账任务执行失败，事务已回滚", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 处理单条对账订单
     */
    private void processSingleReconcileOrder(WithdrawReconcileLog order, LocalDateTime now) {
        // 步骤1：处理业务订单不存在的情况
        if (!(1 == order.getBizOrderExists())) {
            handleBizOrderMissing(order, now);
            return;
        }

        // 步骤2：处理链上数据不存在的情况
        if (StringUtils.isBlank(order.getChainTxHash())) {
            handleChainDataMissing(order, now);
            return;
        }

        // 步骤3：双方数据都存在，执行完整对账
        executeFullReconciliation(order, now);
    }

    /**
     * 处理业务订单缺失
     */
    private void handleBizOrderMissing(WithdrawReconcileLog order, LocalDateTime now) {
        if (order.getBizStatus() == BizStatusEnum.FAIL.getCode()) {
            // 业务订单本身就是失败的，链上无记录是正常的
            order.setReconcileStatus(ReconcileStatusEnum.SUCCESS.getCode());
            order.setReconcileType(ReconcileTypeEnum.FULLY_MATCH.getCode());
            order.setReconcileRemark("对账成功：业务订单为失败，链上没有记录属于正常情况");
        } else {
            order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
            order.setReconcileType(ReconcileTypeEnum.TYPE_CHAIN_HAS_BIZ_NONE.getCode());
            order.setReconcileRemark(String.format("对账异常：链上提现事件存在，但本地无对应提现订单。orderId=%s", order.getBizOrderNumber()));
        }
        order.setReconcileTime(now);
    }

    /**
     * 处理链上数据缺失
     */
    private void handleChainDataMissing(WithdrawReconcileLog order, LocalDateTime now) {
        order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
        order.setReconcileType(ReconcileTypeEnum.TYPE_BIZ_HAS_CHAIN_NONE.getCode());
        order.setReconcileRemark(String.format("对账异常：业务订单存在，但链上无对应提现事件，orderId=%s", order.getBizOrderNumber()));
        order.setReconcileTime(now);
    }

    /**
     * 执行完整对账（双方数据都存在）
     */
    private void executeFullReconciliation(WithdrawReconcileLog order, LocalDateTime now) {
        // 步骤1：比对各字段
        CompareResult result = compareAllFields(order);

        // 步骤2：记录比对结果
        recordComparisonResult(order, result);

        // 步骤3：根据比对结果设置对账状态
        if (result.isAllMatch()) {
            order.setReconcileStatus(ReconcileStatusEnum.SUCCESS.getCode());
            order.setReconcileType(ReconcileTypeEnum.FULLY_MATCH.getCode());
            order.setReconcileRemark("对账成功：链上事件与本地提现订单完全一致");
        } else {
            order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
            order.setReconcileType(determineMismatchType(result));
            order.setReconcileRemark(buildMismatchRemark(order, result));
        }
        order.setReconcileTime(now);
    }

    /**
     * 比对所有字段
     */
    private CompareResult compareAllFields(WithdrawReconcileLog order) {
        CompareResult result = new CompareResult();

        // 比对用户地址
        result.setUserMatch(StringUtils.equalsIgnoreCase(order.getChainUserAddress(), order.getBizToAddress()));

        // 比对金额
        result.setAmountMatch(order.getChainAmount() != null && order.getBizAmount() != null &&
                order.getChainAmount().compareTo(order.getBizAmount()) == 0);

        // 比对提现类型
        result.setRedemptionMatch(order.getChainRedemption() != null && order.getBizRedemption() != null &&
                order.getChainRedemption().intValue() == order.getBizRedemption().intValue());

        // 比对Hash
        result.setHashMatch(StringUtils.equalsIgnoreCase(order.getChainTxHash(), order.getBizHash()));

        // 比对状态
        result.setStatusMatch(order.getBizStatus() != null && order.getBizStatus() == BizStatusEnum.SUCCESS.getCode());

        return result;
    }

    /**
     * 记录比对结果到订单
     */
    private void recordComparisonResult(WithdrawReconcileLog order, CompareResult result) {
        order.setReconcileUserMatch(result.isUserMatch() ? 1 : 0);
        order.setReconcileAmountMatch(result.isAmountMatch() ? 1 : 0);
        order.setReconcileRedemptionMatch(result.isRedemptionMatch() ? 1 : 0);
        order.setReconcileHashMatch(result.isHashMatch() ? 1 : 0);
        order.setReconcileStatusMatch(result.isStatusMatch() ? 1 : 0);
    }

    /**
     * 确定不匹配类型
     */
    private int determineMismatchType(CompareResult result) {
        if (!result.isUserMatch()) return ReconcileTypeEnum.TYPE_USER_MISMATCH.getCode();
        if (!result.isAmountMatch()) return ReconcileTypeEnum.TYPE_AMOUNT_DIFF.getCode();
        if (!result.isRedemptionMatch()) return ReconcileTypeEnum.TYPE_REDEMPTION_MISMATCH.getCode();
        if (!result.isHashMatch()) return ReconcileTypeEnum.TYPE_HASH_DIFF.getCode();
        return ReconcileTypeEnum.FULLY_MATCH.getCode();
    }

    /**
     * 构建不匹配的备注信息
     */
    private String buildMismatchRemark(WithdrawReconcileLog order, CompareResult result) {
        StringBuilder detail = new StringBuilder();
        if (!result.isUserMatch()) {
            detail.append(String.format("用户地址不一致: 链上=%s, 业务=%s; ", order.getChainUserAddress(), order.getBizToAddress()));
        }
        if (!result.isAmountMatch()) {
            detail.append(String.format("金额不一致: 链上=%s, 业务=%s; ", order.getChainAmount(), order.getBizAmount()));
        }
        if (!result.isRedemptionMatch()) {
            detail.append(String.format("提现类型不一致: 链上=%s, 业务=%s; ", order.getChainRedemption(), order.getBizRedemption()));
        }
        if (!result.isHashMatch()) {
            detail.append(String.format("Hash不一致: 链上=%s, 业务=%s; ", order.getChainTxHash(), order.getBizHash()));
        }
        if (!result.isStatusMatch()) {
            detail.append(String.format("提现状态不一致: 链上事件已成功, 业务状态=%s; ", order.getBizStatus()));
        }
        return String.format("对账异常：%s orderId=%s", detail, order.getBizOrderNumber());
    }


}