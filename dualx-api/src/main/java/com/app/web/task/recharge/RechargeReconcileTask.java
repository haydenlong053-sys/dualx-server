package com.app.web.task.recharge;

import com.app.web.service.IRechargeReconcileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



/**
 * 充值订单对账任务
 * <p>
 * 功能说明：
 * 1. 每日统计：统计前一天充值对账数据
 * 2. 实时对账：每分钟执行一次，处理等待20分钟后的待对账订单
 *
 * @author ouyan
 */

@Slf4j
@Component
@Profile("prod")
public class RechargeReconcileTask {

    @Resource
    private IRechargeReconcileService rechargeReconcileService;

    /**
     * 每日统计任务
     * 每天凌晨 0:01 统计前一天的充值对账数据
     */
    @Scheduled(cron = "0 21 0 * * ?")
    public void executeDailyStat() {
        rechargeReconcileService.executeDailyStat();
    }

    /**
     * 自动对账任务
     * 每分钟执行一次，处理等待超过20分钟的待对账订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoReconcileTask() {
        rechargeReconcileService.autoReconcileTask();
    }

}