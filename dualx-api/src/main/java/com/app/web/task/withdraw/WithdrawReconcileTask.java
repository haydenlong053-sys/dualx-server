package com.app.web.task.withdraw;

import com.app.web.service.IWithdrawReconcileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



/**
 * BSC提现对账定时任务
 * <p>
 * 功能说明：
 * 1. 每日统计：统计前一天提现对账数据
 * 2. 实时对账：每分钟执行一次，处理等待20分钟后的待对账订单
 * <p>
 * 对账时机：链上事件和业务订单都已到达后，等待一定时间，再进行最终对账
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Slf4j
@Component
@Profile("prod")
public class WithdrawReconcileTask {

    @Resource
    private IWithdrawReconcileService withdrawReconcileService;

    /**
     * 每日统计任务
     * 每天凌晨 0:01 统计前一天的提现对账数据
     */
    @Scheduled(cron = "0 21 0 * * ?")
    public void executeDailyStat() {
        withdrawReconcileService.executeDailyStat();
    }

    /**
     * 自动对账任务
     * 每分钟执行一次，处理等待超过20分钟的待对账订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoReconcileTask() {
        withdrawReconcileService.autoReconcileTask();
    }
}