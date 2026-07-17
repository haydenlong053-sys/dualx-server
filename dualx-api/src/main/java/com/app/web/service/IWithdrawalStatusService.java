package com.app.web.service;

import com.app.db.entity.BscWithdrawalLog;

/**
 * 提现状态管理器接口
 * <p>
 * 负责订单状态的各种更新操作
 */
public interface IWithdrawalStatusService {

    /**
     * 标记超时订单
     * 检查处理中的订单，根据链上状态更新订单状态
     */
    void markTimeoutOrders();

    /**
     * 处理链上确认中订单
     * 检查链上确认中的订单，如确认成功则更新为成功状态
     */
    void handleChainConfirmingOrders();

    /**
     * 记录为系统错误事件，需要人工手动处理
     * 将订单状态回退到待执行，等待人工介入
     *
     * @param order  订单对象
     * @param remark 备注信息
     */
    void markAsSystemErrorAndRetryLater(BscWithdrawalLog order, String remark);

    /**
     * 标记永久失败
     *
     * @param order  订单对象
     * @param remark 备注信息
     */
    void permanentFail(BscWithdrawalLog order, String remark);
}