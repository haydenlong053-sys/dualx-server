package com.app.web.service;

import com.app.db.entity.PaymentReconcileStat;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 支付对账每日统计表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
public interface IPaymentReconcileStatService extends IService<PaymentReconcileStat> {

    /**
     * 每日统计任务
     */
    void statPreviousDay();

    /**
     * 自动对账任务
     */
    void autoReconcileTask();
}
