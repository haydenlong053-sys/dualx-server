package com.app.web.service;

import com.app.common.dto.WithdrawRequest;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.List;

public interface IWithdrawContractKmsService {

    /**
     * 放弃自己拥有的某个角色权限
     *
     * @param contractAddress 合约地址
     * @param roleHash        角色 bytes32 hash
     * @param fromAddress     当前钱包地址（必须等于 msg.sender）
     * @throws Exception 操作失败时抛出
     */
    void renounceRole(String contractAddress, String roleHash, String fromAddress) throws Exception;

    /**
     * 设置单个用户白名单
     *
     * @param user            用户地址
     * @param allowed         是否允许
     * @param contractAddress 合约地址
     * @throws Exception 操作失败时抛出
     */
    void setAllowedUser(String user, boolean allowed, String contractAddress) throws Exception;

    /**
     * 批量设置用户白名单
     *
     * @param users           用户地址列表
     * @param allowed         是否允许
     * @param contractAddress 合约地址
     * @return 交易回执
     * @throws Exception 操作失败时抛出
     */
    TransactionReceipt batchSetAllowedUsers(List<String> users, boolean allowed, String contractAddress) throws Exception;

    /**
     * 执行提现
     *
     * @param req             提现请求
     * @param signaturesHex   签名列表（十六进制字符串）
     * @param contractAddress 合约地址
     * @return 交易哈希
     * @throws Exception 操作失败时抛出
     */
    String executeWithdraw(WithdrawRequest req, List<String> signaturesHex, String contractAddress) throws Exception;
}