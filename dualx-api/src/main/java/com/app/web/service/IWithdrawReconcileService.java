package com.app.web.service;

/**
 * 提现定时任务接口
 * <p>
 * 统一管理提现相关的定时任务
 */
public interface IWithdrawReconcileService  {


    /**
     * 执行对账核心逻辑
     */
    void autoReconcileTask();

    /**
     * 每日统计任务
     */
    void executeDailyStat();

}