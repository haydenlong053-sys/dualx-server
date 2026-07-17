package com.app.web.service;

import com.alibaba.fastjson.JSONObject;
import com.app.common.dto.PaymentOrderLog;
import com.app.common.model.BaseResult;
import com.app.db.entity.PaymentReconcileLog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.web3j.abi.datatypes.Event;
import org.web3j.protocol.core.methods.response.EthLog;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 支付订单对账中心表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-15
 */
public interface IPaymentReconcileLogService extends IService<PaymentReconcileLog> {

    /**
     * 根据订单号 返回支付订单
     *
     * @param orderNumber 订单号
     * @return 订单
     */
    PaymentReconcileLog getByOrderNumber(String orderNumber);

    /**
     * @param logObject             事件内容
     * @param paySuccessEvent 事件描述
     */
    void savePaySuccessEvent(EthLog.LogObject logObject,Event paySuccessEvent);

    /**
     * 获取待同步MQ的 支付列表
     *
     * @param syncStatusPending 同步状态值
     * @param batchLimit        数量
     * @return 待同步的列表
     */
    List<PaymentReconcileLog> getPendingList(int syncStatusPending, int batchLimit);

    /**
     * 发送mq给业务系统
     *
     */
    void processSyncEvent();

    /**
     * 将mq同步的业务信息 保存或者更新到数据库
     *
     * @param mqMessage mq消息内容
     */
    void saveBizOrder(PaymentOrderLog mqMessage);

    /**
     * 根据订单号查询提现记录
     *
     * @param orderId 订单号
     * @param contractAddress 合约
     * @return 订单详情JSONObject
     */
    JSONObject getWithdrawalByOrderId(String orderId,String contractAddress);

    /**
     * 查询代币兑换价格
     *
     * @param amount 数量
     * @param status 1=ODIC转USDT，2=USDT转ODIC
     * @param tokenAddress 代币合约地址
     * @return 兑换结果
     */
    BaseResult<?> getTokenPrice(BigDecimal amount, Integer status,String tokenAddress);

    void processMessage(String body);
}
