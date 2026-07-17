package com.app.web.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 提现合约配置类
 */
@Data
@Configuration
public class WithdrawContractConfig {

    // ==================== 代币合约配置 ====================
    
    /**
     * 提币代币合约地址（用于查询 allowance）
     */
    @Value("${app.withdrawTokenAddress}")
    private String odicContract;

    /**
     * duon代币合约地址（用于查询 allowance）
     */
    @Value("${app.duonTokenAddress}")
    private String duonContract;

    @Value("${app.imUrl}")
    private String imUrl;

    /**
     * 主网USDT合约地址
     */
    @Value("${app.usdtContract}")
    private String usdtContract;


    // ==================== 提现合约配置 ====================

    /**
     * USDT提现合约地址
     */
    @Value("${contract.exchangeGradeWithdraw.contract-withdraw-u}")
    private String contractWithdrawUsdt;

    /**
     * ODIC提现合约地址
     */
    @Value("${contract.exchangeGradeWithdraw.contract-withdraw-oidc}")
    private String contractWithdrawOdic;

    /**
     * 美區的USDT提现合约地址
     */
    @Value("${contract.exchangeGradeWithdraw.contract-withdraw-u-us}")
    private String usContractWithdrawUsdt;

    /**
     * 美区的ODIC提现合约地址
     */
    @Value("${contract.exchangeGradeWithdraw.contract-withdraw-oidc-us}")
    private String usContractWithdrawOdic;


    /**
     * 美區的USDT提现合约地址
     */
    @Value("${contract.exchangeGradeWithdraw.contract-withdraw-u-product}")
    private String productContractWithdrawUsdt;

    /**
     * 美区的ODIC提现合约地址
     */
    @Value("${contract.exchangeGradeWithdraw.contract-withdraw-oidc-product}")
    private String productContractWithdrawOdic;


    /**
     * 链ID
     */
    @Value("${contract.exchangeGradeWithdraw.chain-id:56}")
    private Integer chainId;


    // ==================== 支付/充值合约配置 ====================

    /**
     * 支付合约地址
     */
    @Value("${payment.payment-contract}")
    private String paymentContractAddress;

    /**
     * 商城的支付合约地址
     */
    @Value("${payment.product-payment-contract}")
    private String paymentProductContract;


    /**
     * 老支付合约地址（充值）
     */
    @Value("${recharge.recharge-contract}")
    private String rechargeContractAddress;

    /**
     * 商城的充值合约
     */
    @Value("${recharge.product-recharge-contract}")
    private String productRechargeContractAddress;



    // ==================== 链上事件扫描配置 ====================

    /**
     * 链上事件起始区块
     */
    @Value("${chain.event.start-block}")
    private Long chainEventStartBlock;

    /**
     * 链上事件确认区块数
     */
    @Value("${chain.event.confirm-blocks:30}")
    private Long chainEventConfirmBlocks;

    @Value("${app.mq.enabled}")
    private Boolean useMq;



}