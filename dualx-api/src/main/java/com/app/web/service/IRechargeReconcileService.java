package com.app.web.service;

/**
 * 充值对账统计定时任务
 */
public interface IRechargeReconcileService {

    /**
     * 每日统计（凌晨0:21）
     */
    void executeDailyStat();

    /**
     * 自动对账（每分钟）
     */
    void autoReconcileTask();
}