package com.app.web.service.impl;

import com.app.common.enums.BizStatusEnum;
import com.app.common.enums.ReconcileStatusEnum;
import com.app.common.enums.ReconcileTypeEnum;
import com.app.common.util.BaseUtil;
import com.app.db.entity.RechargeReconcileLog;
import com.app.web.service.IRechargeReconcileLogService;
import com.app.web.service.IRechargeReconcileService;
import com.app.web.service.IRechargeReconcileStatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RechargeReconcileServiceImpl implements IRechargeReconcileService {

    @Resource
    private IRechargeReconcileLogService rechargeReconcileLogService;
    @Resource
    private IRechargeReconcileStatService rechargeReconcileStatService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void executeDailyStat() {
        log.info("充值统计定时任务开始");
        try {
            rechargeReconcileStatService.statPreviousDay();
            log.info("充值统计定时任务完成");
        } catch (Exception e) {
            log.error("充值统计定时任务失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReconcileTask() {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("充值对账任务仍在运行中，本次跳过");
            return;
        }
        try {
            List<RechargeReconcileLog> pendingList = rechargeReconcileLogService.queryPendingOrders();
            if (!BaseUtil.Base_HasValue(pendingList)) {
                log.debug("无待对账充值订单");
                return;
            }
            log.info("开始充值订单对账，待处理数量={}", pendingList.size());
            List<RechargeReconcileLog> updateList = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (RechargeReconcileLog order : pendingList) {
                processOrder(order, now);
                updateList.add(order);
            }
            if (!updateList.isEmpty()) {
                rechargeReconcileLogService.updateBatchById(updateList);
                log.info("充值订单对账完成，处理数量={}", updateList.size());
            }
        } catch (Exception e) {
            log.error("充值订单对账任务异常，事务将回滚", e);
            throw new RuntimeException("充值对账任务执行失败，事务已回滚", e);
        } finally {
            isRunning.set(false);
        }
    }


    // ==================== 订单处理 ====================

    private void processOrder(RechargeReconcileLog order, LocalDateTime now) {
        order.setReconcileAmountMatch(0);
        order.setReconcileHashMatch(0);
        order.setReconcileStatusMatch(0);
        boolean bizExists = Integer.valueOf(1).equals(order.getBizExists());
        boolean chainExists = Integer.valueOf(1).equals(order.getChainExists());
        // 业务存在，链上不存在
        if (bizExists && !chainExists) {
            handleBizExistsChainNot(order, now);
            return;
        }
        // 链上存在，业务不存在
        if (!bizExists && chainExists) {
            handleChainExistsBizNot(order, now);
            return;
        }
        // 双方都存在，执行详细对账
        performFullReconcile(order, now);
    }

    // ==================== 缺失情况处理 ====================

    private void handleBizExistsChainNot(RechargeReconcileLog order, LocalDateTime now) {
        if (order.getBizStatus() == BizStatusEnum.FAIL.getCode()) {
            order.setReconcileStatus(ReconcileStatusEnum.SUCCESS.getCode());
            order.setReconcileType(ReconcileTypeEnum.FULLY_MATCH.getCode());
            order.setReconcileRemark("对账成功：业务订单为失败，链上没有记录属于正常情况");
        } else {
            order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
            order.setReconcileType(ReconcileTypeEnum.TYPE_BIZ_HAS_CHAIN_NONE.getCode());
            order.setReconcileRemark("业务系统存在充值订单，但未发现对应链上充值事件，需要人工确认");
        }
        order.setReconcileTime(now);
    }

    private void handleChainExistsBizNot(RechargeReconcileLog order, LocalDateTime now) {
        order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
        order.setReconcileType(ReconcileTypeEnum.TYPE_CHAIN_HAS_BIZ_NONE.getCode());
        order.setReconcileRemark("链上存在充值事件，但业务系统未同步对应订单，需要人工确认");
        order.setReconcileTime(now);
    }

    // ==================== 完整对账 ====================

    private void performFullReconcile(RechargeReconcileLog order, LocalDateTime now) {
        // 比对字段
        boolean amountMatch = order.getBizAmount() != null && order.getChainAmount() != null
                && order.getBizAmount().compareTo(order.getChainAmount()) == 0;
        boolean hashMatch = order.getBizHash() != null && order.getChainTxHash() != null
                && order.getBizHash().trim().equalsIgnoreCase(order.getChainTxHash().trim());
        boolean statusMatch = order.getBizStatus() != null && order.getBizStatus() == BizStatusEnum.SUCCESS.getCode();

        // 记录比对结果
        order.setReconcileAmountMatch(amountMatch ? 1 : 0);
        order.setReconcileHashMatch(hashMatch ? 1 : 0);
        order.setReconcileStatusMatch(statusMatch ? 1 : 0);

        // 设置对账结果
        if (amountMatch && hashMatch && statusMatch) {
            order.setReconcileStatus(ReconcileStatusEnum.SUCCESS.getCode());
            order.setReconcileType(ReconcileTypeEnum.FULLY_MATCH.getCode());
            order.setReconcileRemark("对账成功：业务订单与链上充值事件一致");
        } else {
            String mismatchDetail = buildMismatchDetail(order, amountMatch, hashMatch, statusMatch);
            order.setReconcileStatus(ReconcileStatusEnum.FAIL.getCode());
            order.setReconcileType(determineMismatchType(amountMatch, hashMatch, statusMatch));
            order.setReconcileRemark(String.format("对账异常：%s orderId=%s", mismatchDetail, order.getBizOrderNumber()));
        }
        order.setReconcileTime(now);
    }

    private String buildMismatchDetail(RechargeReconcileLog order, boolean amountMatch, boolean hashMatch, boolean statusMatch) {
        StringBuilder detail = new StringBuilder();
        if (!amountMatch) {
            detail.append(String.format("金额不一致: 链上=%s, 业务=%s; ", order.getChainAmount(), order.getBizAmount()));
        }
        if (!hashMatch) {
            detail.append(String.format("Hash不一致: 业务=%s, 链上=%s; ", order.getBizHash(), order.getChainTxHash()));
        }
        if (!statusMatch) {
            detail.append(String.format("充值状态不一致: 链上事件已成功, 业务状态=%s; ", order.getBizStatus()));
        }
        return detail.toString();
    }

    private int determineMismatchType(boolean amountMatch, boolean hashMatch, boolean statusMatch) {
        if (!amountMatch) return ReconcileTypeEnum.TYPE_AMOUNT_DIFF.getCode();
        if (!hashMatch) return ReconcileTypeEnum.TYPE_HASH_DIFF.getCode();
        if (!statusMatch) return ReconcileTypeEnum.TYPE_STATUS_DIFF.getCode();
        return ReconcileTypeEnum.FULLY_MATCH.getCode();
    }
}