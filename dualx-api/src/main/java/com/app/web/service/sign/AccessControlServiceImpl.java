package com.app.web.service.sign;

import com.app.common.dto.WithdrawRequest;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IAccessControlService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;


import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 暂时保留 以后会有管理系统
 * ExchangeGradeWithdrawAccessControl 合约调用服务
 * 功能说明：
 * 查询订单状态、用户 nonce、白名单、signer、角色状态
 * 执行提现 executeWithdraw
 * 设置 whiteList / signer / treasury / threshold / pause
 * 授权和撤销各种管理员角色
 * 本服务对应 AccessControl 多角色权限版合约
 * 不再使用 owner() 管理模型，而是使用 grantRole / revokeRole / renounceRole
 * 不同方法要求 senderAddress 拥有不同角色
 */
@Service
@SuppressWarnings({"rawtypes"})
public class AccessControlServiceImpl implements IAccessControlService {

    /**
     * web3j 链接对象
     */
    @Resource
    private Web3j web3j;
    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    /**
     * 计算提现请求的 EIP-712 签名摘要
     * 对应合约：
     * hashWithdrawRequest(WithdrawRequest req)
     * 所需角色：
     * 无。只读查询，任意地址均可调用 eth_call。
     *
     * @param req 提现请求参数
     * @return digest 十六进制字符串
     * @throws Exception 异常信息
     */
    @Override
    public String hashWithdrawRequest(WithdrawRequest req, String contractAddress) throws Exception {
        Function function = new Function(
                "hashWithdrawRequest",
                Collections.singletonList(req),
                Collections.singletonList(new TypeReference<Bytes32>() {
                })
        );

        List<Type> result = call(function, contractAddress);
        if (result == null || result.isEmpty()) {
            return null;
        }

        Bytes32 bytes32 = (Bytes32) result.get(0);
        return Numeric.toHexString(bytes32.getValue());
    }


    /**
     * 查询订单状态
     * 对应合约：
     * orderStatus(orderId)
     * 返回值：
     * 0 = NONE
     * 1 = EXECUTED
     * 2 = CANCELLED
     * 所需角色：
     * 无。只读查询。
     *
     * @param orderId 合约订单号
     * @return 订单状态
     * @throws Exception 异常信息
     */
    @Override
    public BigInteger getOrderStatus(BigInteger orderId, String contractAddress) throws Exception {
        Function function = new Function(
                "orderStatus",
                Collections.singletonList(new Uint256(orderId)),
                Collections.singletonList(new TypeReference<Uint8>() {
                })
        );

        List<Type> result = call(function, contractAddress);
        if (result == null || result.isEmpty()) {
            return null;
        }

        Uint8 value = (Uint8) result.get(0);
        return value.getValue();
    }


    /**
     * 根据金额查询所需签名数
     * 对应合约：
     * requiredSignaturesForAmount(amount)
     * 所需角色：
     * 无。只读查询。
     *
     * @param amount 提现金额
     * @return 所需签名数
     * @throws Exception 异常信息
     */
    @Override
    public BigInteger requiredSignaturesForAmount(BigInteger amount, String contractAddress) throws Exception {
        Function function = new Function(
                "requiredSignaturesForAmount",
                Collections.singletonList(new Uint256(amount)),
                Collections.singletonList(new TypeReference<Uint256>() {
                })
        );
        return readUint256(function, contractAddress);
    }

    /**
     * 查询某地址是否为 signer
     * 对应合约：
     * isSigner(address)
     * 所需角色：
     * 无。只读查询。
     *
     * @param signerAddress signer 地址
     * @return true=是 false=否
     * @throws Exception 异常信息
     */
    @Override
    public Boolean isSigner(String signerAddress, String contractAddress) throws Exception {
        Function function = new Function(
                "isSigner",
                Collections.singletonList(new Address(signerAddress)),
                Collections.singletonList(new TypeReference<Bool>() {
                })
        );

        List<Type> result = call(function, contractAddress);
        if (result == null || result.isEmpty()) {
            return null;
        }

        return ((Bool) result.get(0)).getValue();
    }

    /**
     * 查询用户是否在提现白名单中
     * 对应合约：
     * allowedUsers(address)
     * 所需角色：
     * 无。只读查询。
     *
     * @param userAddress 用户地址
     * @return true=在白名单 false=不在白名单
     * @throws Exception 异常信息
     */
    @Override
    public Boolean allowedUser(String userAddress, String contractAddress) throws Exception {
        Function function = new Function(
                "allowedUsers",
                Collections.singletonList(new Address(userAddress)),
                Collections.singletonList(new TypeReference<Bool>() {
                })
        );

        List<Type> result = call(function, contractAddress);
        if (result == null || result.isEmpty()) {
            return null;
        }

        return ((Bool) result.get(0)).getValue();
    }

    /**
     * 查询资金池余额
     * 对应合约：
     * getTreasuryBalance(uint8 redemption)
     * redemption:
     * 0 = 正常提币
     * 1 = 闪兑提币
     * 所需角色：
     * 无。只读查询。
     *
     * @param redemption 出账类型
     * @return 资金池余额
     * @throws Exception 异常信息
     */
    @Override
    public BigInteger getTreasuryBalance(BigInteger redemption, String contractAddress) throws Exception {
        Function function = new Function(
                "getTreasuryBalance",
                Collections.singletonList(new Uint8(redemption)),
                Collections.singletonList(new TypeReference<Uint256>() {
                })
        );
        return readUint256(function, contractAddress);
    }


    /**
     * 设置单个用户白名单状态
     * 对应合约：
     * setAllowedUser(address user, bool allowed)
     * 所需角色：
     * RISK_ADMIN_ROLE
     *
     * @param user    用户地址
     * @param allowed 是否允许提现
     * @return 交易回执
     * @throws Exception 异常信息
     */
    @Override
    public TransactionReceipt setAllowedUser(String user, boolean allowed, String contractAddress, String privateKey) throws Exception {
        Function function = new Function(
                "setAllowedUser",
                Arrays.asList(
                        new Address(user),
                        new Bool(allowed)
                ),
                Collections.emptyList()
        );
        return sendTransaction(function, contractAddress, privateKey);
    }

    /**
     * 批量设置用户白名单状态
     * 对应合约：
     * batchSetAllowedUsers(address[] users, bool allowed)
     * 所需角色：
     * RISK_ADMIN_ROLE
     *
     * @param users   用户地址列表
     * @param allowed 是否允许提现
     * @return 交易回执
     * @throws Exception 异常信息
     */
    @Override
    public TransactionReceipt batchSetAllowedUsers(List<String> users, boolean allowed, String contractAddress, String privateKey) throws Exception {
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
        return sendTransaction(function, contractAddress, privateKey);
    }


    /**
     * 执行提现
     * 对应合约：
     * executeWithdraw(WithdrawRequest req, bytes[] signatures)
     * 所需角色：
     * WITHDRAW_EXECUTOR_ROLE
     * 不需要的角色：
     *
     * @param req           提现请求参数
     * @param signaturesHex 签名数组
     * @return 交易回执
     * @throws Exception 异常信息
     */
    @Override
    public TransactionReceipt executeWithdraw(WithdrawRequest req, List<String> signaturesHex, String contractAddress, String privateKey) throws Exception {
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
        return sendTransaction(function, contractAddress, privateKey);
    }


    /**
     * 发送交易并等待回执
     *
     * @param function 合约方法对象
     * @return 交易回执
     * @throws Exception 异常信息
     */
    private TransactionReceipt sendTransaction(Function function, String contractAddress, String privateKey) throws Exception {
        Credentials credentials = Credentials.create(privateKey);
        TransactionManager txManager = new RawTransactionManager(web3j, credentials, withdrawContractConfig.getChainId());

        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger gasPrice = getGasPrice();
        BigInteger gasLimit = DefaultGasProvider.GAS_LIMIT;

        EthSendTransaction ethSendTransaction = txManager.sendTransaction(
                gasPrice,
                gasLimit,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        if (ethSendTransaction == null) {
            throw new RuntimeException("ethSendTransaction is null");
        }

        if (ethSendTransaction.hasError()) {
            throw new RuntimeException("sendTransaction error: " + ethSendTransaction.getError().getMessage());
        }

        String txHash = ethSendTransaction.getTransactionHash();
        if (txHash == null || txHash.isEmpty()) {
            throw new RuntimeException("transaction hash is empty");
        }

        PollingTransactionReceiptProcessor receiptProcessor =
                new PollingTransactionReceiptProcessor(web3j, 3000, 40);

        return receiptProcessor.waitForTransactionReceipt(txHash);
    }


    /**
     * 获取当前 gasPrice
     *
     * @return gasPrice
     * @throws IOException IO异常
     */
    private BigInteger getGasPrice() throws IOException {
        EthGasPrice gasPrice = web3j.ethGasPrice().send();
        if (gasPrice == null || gasPrice.getGasPrice() == null) {
            return DefaultGasProvider.GAS_PRICE;
        }
        return gasPrice.getGasPrice();
    }

    /**
     * 执行只读调用
     *
     * @param function 合约方法对象
     * @return 解码后的返回值列表
     * @throws Exception 异常信息
     */
    private List<Type> call(Function function, String contractAddress) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response == null) {
            throw new RuntimeException("ethCall response is null");
        }

        if (response.hasError()) {
            Response.Error error = response.getError();
            throw new RuntimeException("ethCall error: " + error.getMessage());
        }

        return FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
    }

    /**
     * 读取 uint256 返回值
     *
     * @param function 合约方法对象
     * @return uint256 对应的 BigInteger
     * @throws Exception 异常信息
     */
    private BigInteger readUint256(Function function, String contractAddress) throws Exception {
        List<Type> result = call(function, contractAddress);
        if (result == null || result.isEmpty()) {
            return null;
        }
        Uint256 value = (Uint256) result.get(0);
        return value.getValue();
    }


}