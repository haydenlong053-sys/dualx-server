package com.app.web.service;

import com.app.db.entity.WithdrawReconcileStat;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 提现对账每日统计表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
public interface IWithdrawReconcileStatService extends IService<WithdrawReconcileStat> {

    void statPreviousDay();
}
