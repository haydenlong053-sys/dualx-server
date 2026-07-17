package com.app.web.service;

import com.app.common.dto.WithdrawOrderMessage;
import com.app.db.entity.WithdrawReconcileLog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.core.methods.response.EthLog;

import java.util.List;

/**
 * <p>
 * BSC链上提现成功事件表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-11
 */
public interface IWithdrawReconcileLogService extends IService<WithdrawReconcileLog> {

    /**
     * 保存或者更新MQ同步的提现记录
     *
     * @param withdrawMessage mq内容
     */
    void saveOrUpdateBizOrder(WithdrawOrderMessage withdrawMessage);


    /**
     * @param logObject             事件内容
     * @param contractAddress       合约地址
     * @param contractType          合约类型
     * @param withdrawExecutedEvent 事件描述
     */
    void saveWithdrawExecutedEvent(EthLog.LogObject logObject, String contractAddress, String contractType, Event withdrawExecutedEvent) throws Exception;

    /**
     * 查询待对账的提现记录
     *
     * @return 提现记录列表
     */
    List<WithdrawReconcileLog> queryPendingReconcileOrders();

    void processMessage(String body);
}
