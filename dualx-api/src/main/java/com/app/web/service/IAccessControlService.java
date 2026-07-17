package com.app.web.service;

import com.app.common.dto.WithdrawRequest;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;

/**
 * ExchangeGradeWithdrawAccessControl 合约调用接口
 *
 * 说明：
 * 1. 用于封装 AccessControl 版提现合约的常用查询和写操作
 * 2. 包括提现执行、角色管理、白名单管理、signer 管理、策略配置等
 */
public interface IAccessControlService {

    // =========================================================
    // 只读查询
    // =========================================================

    /**
     * 计算提现请求的 EIP-712 摘要
     */
    String hashWithdrawRequest(WithdrawRequest req,String contractAddress) throws Exception;

    /**
     * 查询订单状态
     */
    BigInteger getOrderStatus(BigInteger orderId,String contractAddress) throws Exception;

    /**
     * 根据金额查询所需签名数
     */
    BigInteger requiredSignaturesForAmount(BigInteger amount,String contractAddress) throws Exception;

    /**
     * 查询某地址是否为 signer
     */
    Boolean isSigner(String signerAddress,String contractAddress) throws Exception;

    /**
     * 查询用户是否在白名单中
     */
    Boolean allowedUser(String userAddress,String contractAddress) throws Exception;

    /**
     * 查询资金池余额
     */
    BigInteger getTreasuryBalance(BigInteger redemption,String contractAddress) throws Exception;

    /**
     * 设置单个用户白名单状态
     * 对应合约：setAllowedUser(address user, bool allowed)
     * 所需角色：RISK_ADMIN_ROLE
     *
     * @param user           用户地址
     * @param allowed        是否允许提现
     * @param contractAddress 合约地址
     * @param privateKey     发送交易的私钥
     * @return 交易回执
     * @throws Exception 异常信息
     */
    TransactionReceipt setAllowedUser(String user, boolean allowed, String contractAddress, String privateKey) throws Exception;

    /**
     * 批量设置用户白名单状态
     * 对应合约：batchSetAllowedUsers(address[] users, bool allowed)
     * 所需角色：RISK_ADMIN_ROLE
     *
     * @param users          用户地址列表
     * @param allowed        是否允许提现
     * @param contractAddress 合约地址
     * @param privateKey     发送交易的私钥
     * @return 交易回执
     * @throws Exception 异常信息
     */
    TransactionReceipt batchSetAllowedUsers(List<String> users, boolean allowed, String contractAddress, String privateKey) throws Exception;

    /**
     * 执行提现
     * 对应合约：executeWithdraw(WithdrawRequest req, bytes[] signatures)
     * 所需角色：WITHDRAW_EXECUTOR_ROLE
     *
     * @param req            提现请求参数
     * @param signaturesHex  签名数组（十六进制字符串格式）
     * @param contractAddress 合约地址
     * @param privateKey     发送交易的私钥
     * @return 交易回执
     * @throws Exception 异常信息
     */
    TransactionReceipt executeWithdraw(WithdrawRequest req, List<String> signaturesHex, String contractAddress, String privateKey) throws Exception;

}