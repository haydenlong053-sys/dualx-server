package com.app.web.service;

import com.app.db.entity.RechargeReconcileStat;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 充值对账每日统计表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
public interface IRechargeReconcileStatService extends IService<RechargeReconcileStat> {

    void statPreviousDay();
}
