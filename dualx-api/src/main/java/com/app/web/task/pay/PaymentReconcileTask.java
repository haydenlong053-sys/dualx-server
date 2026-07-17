package com.app.web.task.pay;

import com.app.web.service.IPaymentReconcileStatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



/**
 * 支付订单对账任务
 *
 * @author ouyan
 */
@Slf4j
@Component
public class PaymentReconcileTask {

    @Resource
    private IPaymentReconcileStatService paymentReconcileStatService;

    /**
     * 每日统计任务
     * 每天凌晨 0:01 统计前一天的支付对账数据
     */
    @Scheduled(cron = "0 21 0 * * ?")
    public void executeDailyStat() {
        paymentReconcileStatService.statPreviousDay();
    }

    /**
     * 自动对账任务
     * 每分钟执行一次，处理等待超过20分钟的待对账订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoReconcileTask() {
        paymentReconcileStatService.autoReconcileTask();
    }
}