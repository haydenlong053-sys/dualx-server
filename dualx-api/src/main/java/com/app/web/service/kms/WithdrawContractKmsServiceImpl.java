package com.app.web.service.kms;

import com.app.common.dto.WithdrawRequest;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IAccessControlService;
import com.app.web.service.IWithdrawContractKmsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 提现合约 KMS 调用服务
 * <p>
 * 包含三个核心功能：
 * 1. 执行签名：使用 withdrawSignKmsSigner
 * 2. 设置白名单：使用 riskAdminKmsTxService
 * 3. 执行提现：使用 withdrawExecutorKmsTxService
 */
@Slf4j
@Service
@SuppressWarnings({"rawtypes"})
public class WithdrawContractKmsServiceImpl implements IWithdrawContractKmsService {

    @Autowired(required = false)
    @Qualifier("withdrawExecutorKmsTxService")
    private KmsTransactionService withdrawExecutorKmsTxService;

    @Autowired(required = false)
    @Qualifier("riskAdminKmsTxService")
    private KmsTransactionService riskAdminKmsTxService;

    @Resource
    private IAccessControlService accessControlService;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;


    /**
     * 2. 设置单个用户白名单
     * <p>
     * 调用合约：
     * setAllowedUser(address user, bool allowed)
     * <p>
     * 发送交易地址：
     * riskAdminKmsTxService 对应的 KMS 地址
     * <p>
     * 链上要求：
     * 该 KMS 地址必须拥有 RISK_ADMIN_ROLE
     */
    @Override
    public void setAllowedUser(String user, boolean allowed, String contractAddress) throws Exception {
        if (!withdrawContractConfig.getUseKms()) {
            accessControlService.setAllowedUser(user, allowed, contractAddress, "0xe5bffd0cde5eeddf0888648de910bf60b83d5c0e5b9f73f0a1a07f2e82d8b8bc");
            return;
        }
        Function function = new Function(
                "setAllowedUser",
                Arrays.asList(
                        new Address(user),
                        new Bool(allowed)
                ),
                Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);

        log.info("准备设置用户白名单, riskAdmin={}, user={}, allowed={}",
                riskAdminKmsTxService.getFromAddress(), user, allowed);

        riskAdminKmsTxService.sendContractTransaction(
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );
    }

    /**
     * 放弃自己拥有的某个角色权限
     * <p>
     * Solidity:
     * renounceRole(bytes32 role, address account)
     * <p>
     * 注意：
     * 1. 只能放弃 msg.sender 自己的权限
     * 2. account 必须等于当前 KMS 钱包地址 fromAddress
     * 3. 如果要移除别人的权限，用 revokeRole，不是 renounceRole
     *
     * @param contractAddress 合约地址
     * @param roleHash        角色 bytes32 hash
     */
    @Override
    public void renounceRole(String contractAddress, String roleHash, String fromAddress) throws Exception {
        if (contractAddress == null || contractAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("contractAddress不能为空");
        }
        if (roleHash == null || !roleHash.startsWith("0x") || roleHash.length() != 66) {
            throw new IllegalArgumentException("roleHash必须是bytes32格式");
        }

        Function function = new Function(
                "renounceRole",
                Arrays.asList(
                        new Bytes32(Numeric.hexStringToByteArray(roleHash)),
                        new Address(fromAddress)
                ),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        withdrawExecutorKmsTxService.sendContractTransaction(
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );
    }

    /**
     * 批量设置用户白名单
     * <p>
     * 调用合约：
     * batchSetAllowedUsers(address[] users, bool allowed)
     */
    @Override
    public TransactionReceipt batchSetAllowedUsers(List<String> users, boolean allowed, String contractAddress) throws Exception {
        if (!withdrawContractConfig.getUseKms()) {
            return accessControlService.batchSetAllowedUsers(users, allowed, contractAddress, "0xe5bffd0cde5eeddf0888648de910bf60b83d5c0e5b9f73f0a1a07f2e82d8b8bc");
        }
        List<Address> addressList = new ArrayList<>();

        for (String user : users) {
            addressList.add(new Address(user));
        }

        Function function = new Function(
                "batchSetAllowedUsers",
                Arrays.asList(
                        new DynamicArray<>(Address.class, addressList),
                        new Bool(allowed)
                ),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        log.info("准备批量设置用户白名单, riskAdmin={}, count={}, allowed={}",
                riskAdminKmsTxService.getFromAddress(), users.size(), allowed);

        return riskAdminKmsTxService.sendContractTransaction(
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );
    }

    /**
     * 3. 执行提现
     * <p>
     * 调用合约：
     * executeWithdraw(WithdrawRequest req, bytes[] signatures)
     * <p>
     * 发送交易地址：
     * withdrawExecutorKmsTxService 对应的 KMS 地址
     * <p>
     * 链上要求：
     * 1. 该 KMS 地址必须拥有 WITHDRAW_EXECUTOR_ROLE
     * 2. req.user 必须在 allowedUsers 白名单
     * 3. signatures 里的 signer 必须在 isSigner 白名单
     */
    @Override
    public String executeWithdraw(WithdrawRequest req, List<String> signaturesHex, String contractAddress) throws Exception {
        /*if (!useKms) {
            return accessControlService.executeWithdraw(req, signaturesHex, contractAddress, "0xe5bffd0cde5eeddf0888648de910bf60b83d5c0e5b9f73f0a1a07f2e82d8b8bc");
        }*/
        List<DynamicBytes> sigList = new ArrayList<>();

        for (String sigHex : signaturesHex) {
            sigList.add(new DynamicBytes(Numeric.hexStringToByteArray(sigHex)));
        }

        Function function = new Function(
                "executeWithdraw",
                Arrays.asList(
                        req,
                        new DynamicArray<>(DynamicBytes.class, sigList)
                ),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        log.info("准备执行提现, executor={}, orderId={}, user={}, amount={}, signatures={}",
                withdrawExecutorKmsTxService.getFromAddress(),
                req.getOrderId(),
                req.getUser(),
                req.getAmount(),
                signaturesHex.size()
        );

        return withdrawExecutorKmsTxService.sendContractTransactionOnlyBroadcast(
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );
    }

}