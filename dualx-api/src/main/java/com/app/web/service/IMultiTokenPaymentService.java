package com.app.web.service;

import com.alibaba.fastjson.JSONObject;
import com.app.common.model.BaseResult;

import java.math.BigDecimal;

/**
 * MultiTokenPayment 合约调用服务接口
 * 功能说明：
 * 查询订单支付状态、币种是否允许、最低支付金额
 * 执行支付
 * 管理允许的币种、暂停/恢复、抢救代币
 */
public interface IMultiTokenPaymentService {
    

    /**
     * 查询订单支付详情
     * 对应合约：getPaymentRecord(string orderId)
     *
     * @param orderId 订单ID
     * @param contractAddress 合约地址
     * @return 支付详情JSON对象，包含：orderId, userAddress, tokenAddress, receiverAddress, amount, timestamp, paymentType
     * @throws Exception 调用失败时抛出
     */
    JSONObject getPaymentRecord(String orderId,String contractAddress) throws Exception;
    
    /**
     * 查询代币对USDT的价格
     *
     * @param tokenAddress 代币合约地址
     * @return 代币价格（USDT计价），查询失败返回null
     */
    BigDecimal getTokenPrice(String tokenAddress);

    /**
     * * 查询订单支付详情
     *
     * @param txHash          hash 值
     * @param contractAddress 合约地址
     * @return 支付详情JSON对象，包含：orderId, userAddress, tokenAddress, receiverAddress, amount, timestamp, paymentType
     */
    BaseResult getPaymentByTxHash(String txHash, String contractAddress);
}