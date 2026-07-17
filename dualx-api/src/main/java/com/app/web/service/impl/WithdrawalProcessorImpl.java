package com.app.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.app.common.constants.RedisConstants;
import com.app.common.dto.WithdrawRequest;
import com.app.common.enums.SignStatusEnum;
import com.app.common.enums.WithdrawalStatusEnum;
import com.app.common.util.RedisUtil;
import com.app.db.entity.BscWithdrawalLog;
import com.app.db.entity.BscWithdrawalSign;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提现订单处理器
 * <p>
 * 负责单笔提现的完整处理流程
 */
@Slf4j
@Service
public class WithdrawalProcessorImpl implements IWithdrawalProcessorService {


    // ==================== 依赖注入 ====================

    @Resource
    private IBscWithdrawalLogService withdrawalLogService;

    @Resource
    private IBscWithdrawalSignService withdrawalSignService;

    @Resource
    private IAccessControlService accessControlService;

    @Resource
    private IWithdrawContractKmsService withdrawContractKmsService;

    @Resource
    private IWithdrawalStatusService withdrawalStatusService;

    @Resource
    private IGasCheckService gasCheckService;
    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    private final Set<String> processingOrders = ConcurrentHashMap.newKeySet();

    @Override
    public boolean process(BscWithdrawalLog order) {
        if (order == null || order.getId() == null) {
            log.warn("提现订单为空或ID为空");
            return true;
        }
        try {
            if (!processingOrders.add(order.getOrderNumber())) {
                log.warn("订单正在处理中，跳过, orderId={}", order.getOrderNumber());
                return true;
            }
            String orderKey = buildOrderKey(order);
            String redisKey = RedisConstants.EXCHANGE_GRADE_WITHDRAW_SEND + orderKey;
            if (!RedisUtil.setNxEx(redisKey, "1", 300)) {
                log.info("提现订单防重拦截, orderKey={}", orderKey);
                return true;
            }

            boolean locked = tryLockOrder(order);
            if (!locked) {
                log.debug("抢单失败，订单已被其他实例处理, orderId={}", order.getId());
                return true;
            }

            log.info("开始处理提现订单, orderId={}, orderNumber={}, amount={}",
                    order.getId(), order.getOrderNumber(), order.getAmount());

            return executeWithdrawal(order);
        } catch (Exception e) {
            log.error("处理提现订单异常, orderId={}", order.getId(), e);
            withdrawalStatusService.markAsSystemErrorAndRetryLater(order, simplifyExceptionMsg(e, "系统异常，等待重试"));
            return true;
        } finally {
            processingOrders.remove(order.getOrderNumber());
        }
    }

    @Override
    public BigInteger convertToChainAmount(BscWithdrawalLog order) {
        try {
            int decimals = getDecimalsByCoinId(order.getCoinId());
            return order.getAmount().multiply(BigDecimal.TEN.pow(decimals)).toBigIntegerExact();
        } catch (Exception e) {
            log.error("金额转换失败, orderId={}, amount={}, coinId={}",
                    order.getId(), order.getAmount(), order.getCoinId(), e);
            return null;
        }
    }

    // ==================== 私有方法 ====================

    private String buildOrderKey(BscWithdrawalLog order) {
        String orderNumber = trimToNull(String.valueOf(order.getOrderNumber()));
        return orderNumber != null ? orderNumber : String.valueOf(order.getId());
    }

    private boolean executeWithdrawal(BscWithdrawalLog order) throws Exception {
        String contractAddress = withdrawalLogService.getContractAddress(order);

        if (!validateBasic(order)) {
            withdrawalStatusService.permanentFail(order, "基础校验不通过");
            return true;
        }

        String idempotentKey = "withdraw:gas:execute:";
        if (RedisUtil.hasKey(idempotentKey)) {
            log.info("Gas或资金池不足，暂停提现, orderId={}", order.getId());
            return false;
        }

        if (!gasCheckService.hasEnoughGas(withdrawContractConfig.getExecuteWithdrawAddress())) {
            log.warn("执行钱包Gas不足，暂停处理后续订单, orderId={}", order.getId());
            RedisUtil.setEx(idempotentKey, "1", 60);
            return false;
        }

        BigInteger chainAmount = convertToChainAmount(order);
        if (chainAmount == null) {
            withdrawalStatusService.permanentFail(order, "提现金额无效");
            return true;
        }

        BigInteger redemption = parseRedemption(order);
        if (redemption == null) {
            withdrawalStatusService.permanentFail(order, "redemption无效");
            return true;
        }

        if (!checkTreasuryBalance(order, redemption, chainAmount)) {
            RedisUtil.setEx(idempotentKey, "1", 60);
            withdrawalStatusService.markAsSystemErrorAndRetryLater(order, "资金池余额不足，等待重试");
            return false;
        }

        if (!accessControlService.allowedUser(order.getToAddress(), contractAddress)) {
            withdrawContractKmsService.setAllowedUser(order.getToAddress(), true, contractAddress);
        }

        BscWithdrawalSign latestSign = getLatestValidSign(order);
        if (latestSign == null) {
            withdrawalStatusService.permanentFail(order, "未找到有效签名记录，签名失败");
            return true;
        }

        WithdrawRequest request = buildWithdrawRequest(order, chainAmount, latestSign);
        log.info("构建签名的request{}", JSONObject.toJSONString(request));

        String SignDigestHex = accessControlService.hashWithdrawRequest(request, contractAddress);
        if (isBlank(SignDigestHex)) {
            withdrawalStatusService.permanentFail(order, "签名摘要计算失败，等待重试");
            return true;
        }

        BigInteger requiredSignatures = accessControlService.requiredSignaturesForAmount(chainAmount, contractAddress);
        List<String> signatures = collectValidSignatures(order, requiredSignatures, SignDigestHex);
        if (signatures == null || signatures.isEmpty()) {
            return true;
        }

        String hash = executeTransaction(request, signatures, order, contractAddress);
        if (hash == null) {
            return true;
        }

        markSuccess(order, hash);
        return true;
    }



    private boolean tryLockOrder(BscWithdrawalLog order) {
        return withdrawalLogService.lambdaUpdate()
                .set(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_PROCESSING.getCode())
                .set(BscWithdrawalLog::getUpdateTime, LocalDateTime.now())
                .set(BscWithdrawalLog::getRemark, "自动发币处理中")
                .eq(BscWithdrawalLog::getId, order.getId())
                .eq(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_PENDING.getCode())
                .update();
    }

    private BscWithdrawalSign getLatestValidSign(BscWithdrawalLog order) {
        LambdaQueryWrapper<BscWithdrawalSign> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BscWithdrawalSign::getFlag, 0)
                .eq(BscWithdrawalSign::getWithdrawLogId, order.getId())
                .eq(BscWithdrawalSign::getSignStatus, SignStatusEnum.SUCCESS.getCode())
                .orderByDesc(BscWithdrawalSign::getId)
                .last("limit 1");
        return withdrawalSignService.getOne(queryWrapper);
    }

    private WithdrawRequest buildWithdrawRequest(BscWithdrawalLog bscWithdrawalLog,
                                                 BigInteger amount, BscWithdrawalSign sign) {
        return new WithdrawRequest(
                BigInteger.valueOf(Long.parseLong(bscWithdrawalLog.getOrderNumber())),
                bscWithdrawalLog.getToAddress(),
                amount,
                BigInteger.valueOf(bscWithdrawalLog.getRedemption().longValue()),
                sign.getDeadline(),
                sign.getBizId()
        );
    }

    private List<String> collectValidSignatures(BscWithdrawalLog order,
                                                BigInteger requiredSignatures, String digestHex) {
        log.info("开始收集有效签名, orderId={}, requiredSignatures={}, digest={}",
                order.getId(), requiredSignatures, digestHex);

        LambdaQueryWrapper<BscWithdrawalSign> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BscWithdrawalSign::getFlag, 0)
                .eq(BscWithdrawalSign::getWithdrawLogId, order.getId())
                .eq(BscWithdrawalSign::getSignStatus, SignStatusEnum.SUCCESS.getCode())
                .orderByAsc(BscWithdrawalSign::getSignerAddress)
                .isNotNull(BscWithdrawalSign::getSignature);

        List<BscWithdrawalSign> signList = withdrawalSignService.list(queryWrapper);

        if (signList == null || signList.isEmpty()) {
            log.warn("未查询到签名记录, orderId={}", order.getId());
            withdrawalStatusService.permanentFail(order, "未找到有效签名记录，退回待签名");
            return null;
        }

        log.info("签名记录查询完成, orderId={}, 原始数量={}", order.getId(), signList.size());

        List<BscWithdrawalSign> validSigns = signList.stream()
                .filter(sign -> !isBlank(sign.getSignerAddress()))
                .filter(sign -> !isBlank(sign.getSignature()))
                .filter(sign -> !isBlank(sign.getSignDigest()))
                .filter(sign -> digestHex.equalsIgnoreCase(sign.getSignDigest()))
                .sorted(this::compareSignerAddress)
                .collect(java.util.stream.Collectors.toList());

        log.info("签名摘要匹配并排序完成, orderId={}, digest匹配后数量={}",
                order.getId(), validSigns.size());

        if (validSigns.isEmpty()) {
            log.warn("没有匹配当前摘要的有效签名, orderId={}, digest={}", order.getId(), digestHex);
            withdrawalStatusService.permanentFail(order, "未找到匹配当前摘要的有效签名");
            return null;
        }

        List<BscWithdrawalSign> dedupSigns = new ArrayList<>();
        String lastSignerAddress = null;

        for (BscWithdrawalSign sign : validSigns) {
            String currentSignerAddress = normalizeAddress(sign.getSignerAddress());
            if (!currentSignerAddress.equals(lastSignerAddress)) {
                dedupSigns.add(sign);
                lastSignerAddress = currentSignerAddress;
            } else {
                log.warn("发现重复签名人，已忽略重复记录, orderId={}, signerAddress={}, signId={}",
                        order.getId(), currentSignerAddress, sign.getId());
            }
        }

        log.info("签名去重完成, orderId={}, 去重后数量={}", order.getId(), dedupSigns.size());

        if (BigInteger.valueOf(dedupSigns.size()).compareTo(requiredSignatures) < 0) {
            log.warn("有效签名数量不足, orderId={}, requiredSignatures={}, actualCount={}",
                    order.getId(), requiredSignatures, dedupSigns.size());
            withdrawalStatusService.permanentFail(order, String.format("有效签名数量不足，需要%s个，实际%s个", requiredSignatures, dedupSigns.size()));
            return null;
        }

        List<String> signatures = new ArrayList<>();
        for (int i = 0; i < requiredSignatures.intValue(); i++) {
            BscWithdrawalSign sign = dedupSigns.get(i);
            if (isBlank(sign.getSignature())) {
                log.error("存在空签名记录, orderId={}, signerAddress={}, signId={}",
                        order.getId(), sign.getSignerAddress(), sign.getId());
                withdrawalStatusService.markAsSystemErrorAndRetryLater(order, "存在空签名记录，等待重试");
                return null;
            }
            signatures.add(sign.getSignature());
            log.info("选中签名记录, orderId={}, signerAddress={}, signId={}, index={}",
                    order.getId(), normalizeAddress(sign.getSignerAddress()), sign.getId(), i);
        }

        log.info("有效签名收集成功, orderId={}, 实际使用签名数={}", order.getId(), signatures.size());
        return signatures;
    }

    private int compareSignerAddress(BscWithdrawalSign a, BscWithdrawalSign b) {
        BigInteger addrA = addressToBigInteger(normalizeAddress(a.getSignerAddress()));
        BigInteger addrB = addressToBigInteger(normalizeAddress(b.getSignerAddress()));
        return addrA.compareTo(addrB);
    }

    private BigInteger addressToBigInteger(String address) {
        if (isBlank(address)) {
            throw new IllegalArgumentException("签名人地址为空");
        }
        String normalized = normalizeAddress(address);
        if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }
        return new BigInteger(normalized, 16);
    }

    private String normalizeAddress(String address) {
        if (isBlank(address)) {
            return "";
        }
        String normalized = address.trim().toLowerCase();
        if (!normalized.startsWith("0x")) {
            normalized = "0x" + normalized;
        }
        return normalized;
    }

    private String executeTransaction(WithdrawRequest request,
                                      List<String> signatures,
                                      BscWithdrawalLog order,
                                      String contractAddress) {
        try {
            String txHash = withdrawContractKmsService.executeWithdraw(request, signatures, contractAddress);
            if (txHash == null || txHash.trim().isEmpty()) {
                log.error("链上交易广播失败，txHash为空, orderId={}", order.getId());
                withdrawalStatusService.markAsSystemErrorAndRetryLater(order, "链上交易广播失败，等待重试");
                return null;
            }
            return txHash;
        } catch (Exception e) {
            log.error("发送链上交易异常, orderId={}", order.getId(), e);
            withdrawalStatusService.markAsSystemErrorAndRetryLater(order, simplifyExceptionMsg(e, "发送链上交易失败，等待重试"));
            return null;
        }
    }

    private void markSuccess(BscWithdrawalLog order, String hash) {
        boolean updated = withdrawalLogService.lambdaUpdate()
                .set(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_CHAIN_CONFIRMING.getCode())
                .set(BscWithdrawalLog::getHash, hash)
                .set(BscWithdrawalLog::getUpdateTime, LocalDateTime.now())
                .set(BscWithdrawalLog::getRemark, "链上确认中")
                .eq(BscWithdrawalLog::getId, order.getId())
                .eq(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_PROCESSING.getCode())
                .update();
        RedisUtil.setEx(order.getOrderNumber() + "_hash", hash, 24 * 60 * 60L);
        if (updated) {
            log.info("提现成功, orderId={}, orderNumber={}, txHash={}",
                    order.getId(), order.getOrderNumber(), hash);
        } else {
            log.warn("提现成功但状态更新失败, orderId={}", order.getId());
        }
    }

    private int getDecimalsByCoinId(Integer coinId) {
        if (coinId == null) {
            throw new IllegalArgumentException("coinId不能为空");
        }
        if (coinId == 1 || coinId == 2) {
            return 18;
        }
        throw new IllegalArgumentException("未知coinId: " + coinId);
    }

    private BigInteger parseRedemption(BscWithdrawalLog order) {
        if (order.getRedemption() == null) {
            return null;
        }
        return BigInteger.valueOf(order.getRedemption().longValue());
    }

    private String simplifyExceptionMsg(Exception e, String defaultMsg) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return defaultMsg;
        }
        String msg = e.getMessage().trim();
        if (msg.length() > 80) {
            msg = msg.substring(0, 80);
        }
        if (msg.contains("insufficient funds")) {
            return "手续费不足，等待重试";
        }
        if (msg.contains("nonce too low")) {
            return "nonce冲突，等待重试";
        }
        if (msg.contains("replacement transaction underpriced")) {
            return "交易价格过低，等待重试";
        }
        if (msg.contains("execution reverted")) {
            return "合约执行失败，等待人工检查";
        }
        return defaultMsg;
    }

    /**
     * 基础校验
     */
    public boolean validateBasic(BscWithdrawalLog order) {
        // 校验提现金额
        if (order.getAmount() == null) {
            log.warn("提现金额为空, orderId={}", order.getId());
            return false;
        }
        if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("提现金额错误, orderId={}", order.getId());
            return false;
        }

        // 校验提现地址
        String userAddress = trimToNull(order.getToAddress());
        if (userAddress == null) {
            log.warn("提现地址为空, orderId={}", order.getId());
            return false;
        }

        return true;
    }

    /**
     * 检查资金池余额
     */
    public boolean checkTreasuryBalance(BscWithdrawalLog order, BigInteger redemption, BigInteger amount) {
        try {
            String contractAddress = withdrawalLogService.getContractAddress(order);
            BigInteger treasuryBalance = accessControlService.getTreasuryBalance(redemption, contractAddress);
            if (treasuryBalance == null || treasuryBalance.compareTo(amount) < 0) {
                log.warn("资金池余额不足, orderId={}, need={}, balance={}",
                        order.getId(), amount, treasuryBalance);
                return false;
            }
            log.debug("资金池余额充足, orderId={}, balance={}", order.getId(), treasuryBalance);
            return true;

        } catch (Exception e) {
            log.error("查询资金池余额失败, orderId={}, redemption={}", order.getId(), redemption, e);
            return false;
        }
    }


    private String trimToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private boolean isBlank(String s) {
        return trimToNull(s) == null;
    }
}