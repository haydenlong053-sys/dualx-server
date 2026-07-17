package com.app.web.task.exchange;

import com.app.common.enums.WithdrawalStatusEnum;
import com.app.common.util.RedisUtil;
import com.app.db.entity.BscWithdrawalLog;
import com.app.web.service.IAccessControlService;
import com.app.web.service.IBscWithdrawalLogService;
import com.app.web.service.IWithdrawalStatusService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提现状态管理器
 * <p>
 * 负责订单状态的各种更新操作
 */
@Slf4j
@Component
public class WithdrawalStatusServiceImpl implements IWithdrawalStatusService {

    @Resource
    private IBscWithdrawalLogService withdrawalLogService;

    @Resource
    private IAccessControlService accessControlService;


    /**
     * 标记超时订单
     */
    @Override
    public void markTimeoutOrders() {
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(25);
        //获取提现中或者需要人工介入的订单 然后根据订单ID 去合约里查一下是否发币成功
        List<BscWithdrawalLog> timeoutOrders = withdrawalLogService.getDeadlineOrder(timeoutTime, 100);
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return;
        }
        log.info("发现 {} 笔处理中超时订单", timeoutOrders.size());
        for (BscWithdrawalLog order : timeoutOrders) {
            String contractAddress = withdrawalLogService.getContractAddress(order);
            //去合约里面查询是否成功 如果成功则修改为成功 否则修改为过期订单
            try {
                BigInteger status = accessControlService.getOrderStatus(new BigInteger(String.valueOf(order.getOrderNumber())), contractAddress);
                //说明发币成功了
                if (status.compareTo(BigInteger.ONE) == 0) {
                    RedisUtil.setEx(order.getOrderNumber() + "_status", "2", 24 * 60 * 60L);
                    markStatus(order, WithdrawalStatusEnum.STATUS_SUCCESS, "发币成功");
                } else if (status.compareTo(BigInteger.ZERO) == 0) {
                    markStatus(order, WithdrawalStatusEnum.STATUS_PENDING, "需要重新发币");
                } else {
                    markStatus(order, WithdrawalStatusEnum.STATUS_TIMEOUT, "超时处理");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 处理链上确认中订单
     */
    @Override
    public void handleChainConfirmingOrders() {
        List<BscWithdrawalLog> chainOrders = withdrawalLogService.lambdaQuery()
                .eq(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_CHAIN_CONFIRMING.getCode())
                .last("limit " + 100)
                .list();

        if (chainOrders == null || chainOrders.isEmpty()) {
            return;
        }
        log.info("发现 {} 笔链上确认中订单", chainOrders.size());
        for (BscWithdrawalLog order : chainOrders) {
            try {
                String contractAddress = withdrawalLogService.getContractAddress(order);
                BigInteger status = accessControlService.getOrderStatus(new BigInteger(String.valueOf(order.getOrderNumber())), contractAddress);
                if (BigInteger.ONE.compareTo(status) == 0) {
                    RedisUtil.setEx(order.getOrderNumber() + "_status", "2", 24 * 60 * 60L);
                    markStatus(order, WithdrawalStatusEnum.STATUS_SUCCESS, "发币成功");
                }
            } catch (Exception e) {
                log.error("处理链上确认中订单异常, orderId={}", order.getId(), e);
            }
        }
    }


    /**
     * 标记单笔订单超时
     */
    private void markStatus(BscWithdrawalLog order, WithdrawalStatusEnum statusEnum, String remark) {
        try {
            boolean updated = withdrawalLogService.markStatus(order.getId(), statusEnum, remark);
            if (updated) {
                log.warn("订单标记为超时, orderId={}, orderNumber={}", order.getId(), order.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("标记超时订单失败, orderId={}", order.getId(), e);
        }
    }

    /**
     * 记录为系统错误事件，需要人工手动处理
     * 将订单状态回退到待执行，等待人工介入
     */
    @Override
    public void markAsSystemErrorAndRetryLater(BscWithdrawalLog order, String remark) {
        boolean updated = withdrawalLogService.markStatus(order.getId(), WithdrawalStatusEnum.SYSTEM_ERROR, remark);
        if (updated) {
            log.info("订单标记为系统错误，等待人工处理, orderId={}, remark={}", order.getId(), remark);
        } else {
            log.warn("订单标记系统错误失败（状态可能已变更）, orderId={}", order.getId());
        }
    }


    /**
     * 标记永久失败
     */
    @Override
    public void permanentFail(BscWithdrawalLog order, String remark) {
        boolean updated = withdrawalLogService.markStatus(order.getId(), WithdrawalStatusEnum.STATUS_FAIL, truncateRemark(remark));
        if (updated) {
            log.error("订单标记为永久失败, orderId={}, remark={}", order.getId(), remark);
        }
    }

    /**
     * 截断备注信息（限制长度）
     */
    private String truncateRemark(String remark) {
        if (remark == null) {
            return "";
        }
        remark = remark.trim();
        if (remark.length() > 100) {
            return remark.substring(0, 100);
        }
        return remark;
    }
}