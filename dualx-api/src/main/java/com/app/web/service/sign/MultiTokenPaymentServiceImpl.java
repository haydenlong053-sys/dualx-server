package com.app.web.service.sign;

import com.alibaba.fastjson.JSONObject;
import com.app.common.constants.RedisConstants;
import com.app.common.constants.SysConfigConstants;
import com.app.common.enums.TxResultCodeEnum;
import com.app.common.model.BaseResult;
import com.app.common.util.BaseUtil;
import com.app.common.util.RedisUtil;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IMultiTokenPaymentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MultiTokenPayment 合约调用服务
 * 功能说明：
 * 查询订单支付状态、币种是否允许、最低支付金额
 * 执行支付
 * 管理允许的币种、暂停/恢复、抢救代币
 */
@Service
@SuppressWarnings({"rawtypes"})
@Slf4j
public class MultiTokenPaymentServiceImpl implements IMultiTokenPaymentService {

    @Resource
    private Web3j web3j;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;


    /**
     * 查询订单支付详情
     * 对应合约：getPaymentRecord(string orderId)
     *
     * @param orderId 订单ID
     * @return 支付详情
     */
    public JSONObject getPaymentRecord(String orderId, String contractAddress) throws Exception {
        Function function = new Function(
                "getPaymentRecord",
                Collections.singletonList(new Utf8String(orderId)),
                Arrays.asList(
                        new TypeReference<Utf8String>() {
                        }, // orderId
                        new TypeReference<Address>() {
                        },    // user
                        new TypeReference<Address>() {
                        },    // token
                        new TypeReference<Address>() {
                        },    // receiver
                        new TypeReference<Uint256>() {
                        },    // amount
                        new TypeReference<Uint256>() {
                        },    // timestamp
                        new TypeReference<Uint256>() {
                        },   // paymentType（新增）
                        new TypeReference<Uint256>() {
                        }  //  // source（新增）

                )
        );

        List<Type> result = call(function, contractAddress);

        if (result == null || result.size() < 7) {  // 改为 7
            return null;
        }

        String recordOrderId = ((Utf8String) result.get(0)).getValue();
        String userAddress = ((Address) result.get(1)).getValue();
        String tokenAddress = ((Address) result.get(2)).getValue();
        String receiverAddress = ((Address) result.get(3)).getValue();
        BigInteger rawAmount = (BigInteger) result.get(4).getValue();
        String timestamp = result.get(5).getValue().toString();
        BigInteger paymentType = (BigInteger) result.get(6).getValue();  // 新增：获取支付类型
        BigInteger source = (BigInteger) result.get(7).getValue();  // 新增：获取支付类型

        // 转成人类金额（18位精度）
        BigDecimal humanAmount = new BigDecimal(rawAmount)
                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.DOWN);
        String amount = humanAmount.stripTrailingZeros().toPlainString();

        JSONObject messageBody = new JSONObject();
        messageBody.put("orderId", recordOrderId);
        messageBody.put("source", source);
        messageBody.put("userAddress", userAddress);
        messageBody.put("tokenAddress", tokenAddress);
        messageBody.put("receiverAddress", receiverAddress);
        messageBody.put("amount", amount);
        messageBody.put("timestamp", String.valueOf(Long.parseLong(timestamp) / 1000));
        messageBody.put("paymentType", paymentType);  // 新增：返回支付类型

        return messageBody;
    }

    /**
     * 执行只读调用
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
     * 查询代币对USDT的价格
     *
     * @param tokenAddress 代币合约地址
     * @return 代币价格（USDT计价），查询失败返回null
     */
    public BigDecimal getTokenPrice(String tokenAddress) {
        try {
            // 1. 查询代币精度
            int decimals = getTokenDecimals(tokenAddress);
            if (decimals == 0) {
                log.warn("获取代币精度失败, tokenAddress={}", tokenAddress);
                return null;
            }
            // 2. 构建查询路径 token -> USDT
            List<Address> path = Arrays.asList(
                    new Address(tokenAddress),
                    new Address(withdrawContractConfig.getUsdtContract())
            );
            BigInteger amountIn = BigInteger.TEN.pow(decimals);
            Function function = new Function(
                    "getAmountsOut",
                    Arrays.asList(
                            new Uint256(amountIn),
                            new DynamicArray<>(Address.class, path)
                    ),
                    Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {
                    })
            );

            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, SysConfigConstants.PANCAKE_ROUTER, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                log.warn("查询价格失败: {}", response.getError().getMessage());
                return null;
            }
            List<Type> result = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );
            @SuppressWarnings("unchecked")
            DynamicArray<Uint256> amounts = (DynamicArray<Uint256>) result.get(0);
            BigInteger usdtAmount = amounts.getValue().get(1).getValue();
            return new BigDecimal(usdtAmount).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("查询代币价格异常, tokenAddress={}", tokenAddress, e);
            return null;
        }
    }


    /**
     * 根据交易哈希查询支付信息
     */
    @Override
    public BaseResult getPaymentByTxHash(String txHash, String contractAddress) {
        try {
            // 参数校验 - 交易哈希为空
            if (!BaseUtil.Base_HasValue(txHash)) {
                JSONObject result = new JSONObject();
                result.put("paymentStatus", 2);
                result.put("message", "交易哈希不能为空");
                return BaseResult.success("查询完成", result);
            }
            // 参数校验 - 合约地址为空
            if (!BaseUtil.Base_HasValue(contractAddress)) {
                JSONObject result = new JSONObject();
                result.put("paymentStatus", 2);
                result.put("message", "合约地址不能为空");
                return BaseResult.success("查询完成", result);
            }
            // 频率限制
            String limitKey = RedisConstants.PAYMENT_HASH_QUERY_LIMIT_PREFIX + contractAddress.trim().toLowerCase() + ":" + txHash.trim().toLowerCase();
            boolean canQuery = RedisUtil.setNxEx(limitKey, "1", 3);
            if (!canQuery) {
                JSONObject result = new JSONObject();
                result.put("paymentStatus", 0);
                result.put("message", "查询过于频繁，请3秒后再试");
                return BaseResult.success("查询中", result);
            }
            // 1. 查缓存（已确认的交易）
            if (RedisUtil.hasKey(RedisConstants.PAYMENT_HASH + txHash)) {
                String data = RedisUtil.get(RedisConstants.PAYMENT_HASH + txHash);
                JSONObject result = JSONObject.parseObject(data);
                return BaseResult.success("查询支付成功", result);
            }
            // 2. 查询交易收据
            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(txHash).send();
            // 2.1 交易不存在或未上链 -> 统一返回"链上查询中"
            if (!receiptResponse.getTransactionReceipt().isPresent()) {
                JSONObject result = new JSONObject();
                result.put("txHash", txHash);
                result.put("paymentStatus", 0);
                result.put("message", "交易已广播，等待矿工打包确认");
                return BaseResult.success("链上查询中", result);
            }
            TransactionReceipt receipt = receiptResponse.getTransactionReceipt().get();
            String status = receipt.getStatus();
            // 2.2 交易执行失败 (revert)
            if (status == null || "0x0".equals(status) || "0".equals(status)) {
                String revertReason = getRevertReason(receipt);
                TxResultCodeEnum errorEnum = parseRevertReason(revertReason);
                JSONObject result = new JSONObject();
                result.put("txHash", txHash);
                result.put("paymentStatus", 2);
                result.put("errorCode", errorEnum.getCode());
                result.put("message", errorEnum.getRemark());
                return BaseResult.success("查询完成", result);
            }
            // 2.3 检查区块确认数
            BigInteger blockNumber = receipt.getBlockNumber();
            if (blockNumber != null) {
                BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
                BigInteger confirmations = currentBlock.subtract(blockNumber).add(BigInteger.ONE);
                if (confirmations.compareTo(BigInteger.valueOf(12)) < 0) {
                    JSONObject result = new JSONObject();
                    result.put("txHash", txHash);
                    result.put("paymentStatus", 0);
                    result.put("confirmations", confirmations.toString());
                    result.put("message", "交易已上链，等待确认中，当前确认数: " + confirmations);
                    return BaseResult.success("链上查询中", result);
                }
            }
            // 3. 交易已确认（>=12个确认），解析事件
            for (Log event : receipt.getLogs()) {
                if (StringUtils.isNotBlank(contractAddress) && !contractAddress.equalsIgnoreCase(event.getAddress())) {
                    continue;
                }
                List<String> topics = event.getTopics();
                if (topics == null || topics.size() < 4) {
                    continue;
                }
                String paySuccessEventSig = "0xad35f9c189ee5b9838253bfe5bae62f54743365213a52260a0e237196db47cad";
                if (!paySuccessEventSig.equalsIgnoreCase(topics.get(0))) {
                    continue;
                }
                String user = "0x" + topics.get(1).substring(26);
                String token = "0x" + topics.get(2).substring(26);
                String receiver = "0x" + topics.get(3).substring(26);
                List<TypeReference<Type>> parameters = Arrays.asList(
                        typeRef(new TypeReference<Utf8String>() {}),
                        typeRef(new TypeReference<Uint256>() {}),
                        typeRef(new TypeReference<Uint256>() {}),
                        typeRef(new TypeReference<Uint256>() {}),
                        typeRef(new TypeReference<Uint256>() {})
                );

                List<Type> decodedData = FunctionReturnDecoder.decode(event.getData(), parameters);
                if (decodedData == null || decodedData.size() < 5) {
                    log.warn("解析 PaySuccess data 失败, txHash: {}, data: {}", txHash, event.getData());
                    continue;
                }
                String orderId = (String) decodedData.get(0).getValue();
                BigInteger amount = (BigInteger) decodedData.get(1).getValue();
                BigInteger timestamp = (BigInteger) decodedData.get(2).getValue();
                BigInteger paymentType = (BigInteger) decodedData.get(3).getValue();
                BigInteger source = (BigInteger) decodedData.get(4).getValue();
                BigDecimal humanAmount = new BigDecimal(amount).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.DOWN);
                String amountText = humanAmount.stripTrailingZeros().toPlainString();
                JSONObject result = new JSONObject();
                result.put("txHash", txHash);
                result.put("orderId", orderId);
                result.put("userAddress", user);
                result.put("tokenAddress", token);
                result.put("receiverAddress", receiver);
                result.put("amount", amountText);
                result.put("rawAmount", amount.toString());
                result.put("timestamp", timestamp.toString());
                result.put("paymentType", paymentType.toString());
                result.put("source", source.toString());
                result.put("paymentStatus", 1);
                RedisUtil.setEx(RedisConstants.PAYMENT_HASH + txHash, result.toJSONString(), SysConfigConstants.REDIS_ONE_DAY_SECONDS);
                return BaseResult.success("查询支付成功", result);
            }
            // 4. 交易成功但没有匹配的事件
            JSONObject result = new JSONObject();
            result.put("txHash", txHash);
            result.put("paymentStatus", 2);
            result.put("message", "未找到支付事件");
            return BaseResult.success("查询完成", result);
        } catch (Exception e) {
            log.error("查询交易异常, txHash: {}", txHash, e);
            JSONObject result = new JSONObject();
            result.put("txHash", txHash);
            result.put("paymentStatus", 2);
            result.put("message", "查询交易异常: " + e.getMessage());
            return BaseResult.success("查询异常", result);
        }
    }



    /**
     * 获取 revert 原因
     */
    private String getRevertReason(TransactionReceipt receipt) {
        try {
            String revertReason = receipt.getRevertReason();
            if (revertReason != null && !revertReason.isEmpty()) {
                return revertReason;
            }
        } catch (Exception e) {
            log.warn("获取 revert 原因失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析 revert 原因，返回对应的错误枚举
     */
    private TxResultCodeEnum parseRevertReason(String revertReason) {
        if (revertReason == null) {
            return TxResultCodeEnum.TX_EXECUTION_FAILED;
        }
        if (revertReason.contains("InvalidSource")) {
            return TxResultCodeEnum.INVALID_SOURCE;
        }
        if (revertReason.contains("InvalidPaymentType")) {
            return TxResultCodeEnum.INVALID_PAYMENT_TYPE;
        }
        if (revertReason.contains("TokenNotAllowed")) {
            return TxResultCodeEnum.TOKEN_NOT_ALLOWED;
        }
        if (revertReason.contains("AmountTooSmall")) {
            return TxResultCodeEnum.AMOUNT_TOO_SMALL;
        }
        if (revertReason.contains("OrderAlreadyExists")) {
            return TxResultCodeEnum.ORDER_ALREADY_EXISTS;
        }
        if (revertReason.contains("OrderNotExists")) {
            return TxResultCodeEnum.ORDER_NOT_EXISTS;
        }
        if (revertReason.contains("ContractPaused")) {
            return TxResultCodeEnum.CONTRACT_PAUSED;
        }
        if (revertReason.contains("ZeroAddress")) {
            return TxResultCodeEnum.ZERO_ADDRESS;
        }
        if (revertReason.contains("ZeroAmount")) {
            return TxResultCodeEnum.ZERO_AMOUNT;
        }
        if (revertReason.contains("NotAdmin")) {
            return TxResultCodeEnum.NOT_ADMIN;
        }
        return TxResultCodeEnum.TX_EXECUTION_FAILED;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TypeReference<Type> typeRef(TypeReference ref) {
        return (TypeReference<Type>) ref;
    }

    /**
     * 查询代币精度
     */
    private int getTokenDecimals(String tokenAddress) throws Exception {
        String encodedFunction = FunctionEncoder.encode(
                new Function("decimals", Collections.emptyList(), Collections.emptyList())
        );

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        String hexValue = response.getValue();
        BigInteger decimals = new BigInteger(hexValue.substring(2), 16);
        return decimals.intValue();
    }


}