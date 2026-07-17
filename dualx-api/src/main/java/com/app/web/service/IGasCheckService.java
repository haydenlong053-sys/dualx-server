package com.app.web.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public interface IGasCheckService {
    
    /**
     * 自动检测gas费和资金池有没有足够的金额
     */
    void gas();
    
    /**
     * 获取代币价格和钱包余额信息
     */
    void getTokenPrice() throws Exception;
    
    /**
     * Wei转ETH
     */
    BigDecimal weiToEth(BigInteger wei);
    
    /**
     * 检查执行钱包是否有足够的Gas
     * @param executorAddress 执行钱包地址
     * @return true-足够, false-不足
     */
    boolean hasEnoughGas(String executorAddress);
    
    /**
     * 查询原生币余额
     * @param address 钱包地址
     * @return 余额（Wei）
     */
    BigInteger getNativeBalance(String address) throws IOException;
}