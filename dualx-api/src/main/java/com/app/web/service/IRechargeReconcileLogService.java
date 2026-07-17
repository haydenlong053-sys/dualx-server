package com.app.web.service;

import com.app.common.dto.RechargeOrderLogDTO;
import com.app.db.entity.RechargeReconcileLog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.core.methods.response.EthLog;

import java.util.List;

/**
 * <p>
 * 支付订单对账中心表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
public interface IRechargeReconcileLogService extends IService<RechargeReconcileLog> {

    RechargeReconcileLog getByOrderNumber(String orderNumber);

    void saveRechargeSuccess(EthLog.LogObject logObject, Event oldPaymentEvent);

    void saveBizOrder(RechargeOrderLogDTO mqMessage);

    /**
     * 查询待对账的订单
     *
     * @return 订单列表
     */
    List<RechargeReconcileLog> queryPendingOrders();

    void processMessage(String body);
}
