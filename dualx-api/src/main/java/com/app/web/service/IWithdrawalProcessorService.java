package com.app.web.service;

import com.app.db.entity.BscWithdrawalLog;
import java.math.BigInteger;

/**
 * 提现订单处理器接口
 */
public interface IWithdrawalProcessorService {
    
    /**
     * 处理单笔提现订单
     *
     * @param order 提现订单
     * @return true-继续处理下一笔, false-资源不足停止处理
     */
    boolean process(BscWithdrawalLog order);
    
    /**
     * 金额转换为链上最小单位
     */
    BigInteger convertToChainAmount(BscWithdrawalLog order);
}