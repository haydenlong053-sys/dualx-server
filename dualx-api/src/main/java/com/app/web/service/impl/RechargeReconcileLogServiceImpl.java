package com.app.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.app.common.constants.SysConfigConstants;
import com.app.common.dto.RechargeOrderLogDTO;
import com.app.common.enums.ReconcileStatusEnum;
import com.app.db.entity.RechargeReconcileLog;
import com.app.db.mapper.RechargeReconcileLogMapper;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IRechargeReconcileLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthLog;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 充值订单对账中心表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Slf4j
@Service
public class RechargeReconcileLogServiceImpl extends ServiceImpl<RechargeReconcileLogMapper, RechargeReconcileLog> implements IRechargeReconcileLogService {

    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    /**
     * 根据业务订单号查询对账记录
     */
    @Override
    public RechargeReconcileLog getByOrderNumber(String orderNumber) {
        return getOne(new LambdaQueryWrapper<RechargeReconcileLog>()
                .eq(RechargeReconcileLog::getBizOrderNumber, orderNumber)
                .last("limit 1"));
    }

    /**
     * 保存老支付合约充值成功事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRechargeSuccess(EthLog.LogObject logObject, Event OLD_PAYMENT_EVENT) {
        String txHash = logObject.getTransactionHash().toLowerCase();
        Long logIndex = logObject.getLogIndex().longValue();

        if (existsByTxHashAndLogIndex(txHash, logIndex)) {
            log.info("充值事件已存在，跳过, txHash={}, logIndex={}", txHash, logIndex);
            return;
        }

        List<Type> dataList = FunctionReturnDecoder.decode(
                logObject.getData(),
                OLD_PAYMENT_EVENT.getNonIndexedParameters()
        );

        if (dataList == null || dataList.size() < 3) {
            throw new RuntimeException("PaymentMade data 解析失败");
        }
        BigInteger coinId = (BigInteger) dataList.get(0).getValue();
        BigInteger orderId = (BigInteger) dataList.get(1).getValue();
        BigInteger amount = (BigInteger) dataList.get(2).getValue();
        BigInteger source = (BigInteger) dataList.get(3).getValue();
        String orderNumber = orderId.toString();
        RechargeReconcileLog reconcileLog = getByOrderNumber(orderNumber);
        if (reconcileLog == null) {
            reconcileLog = new RechargeReconcileLog();
            reconcileLog.setCreateTime(LocalDateTime.now());
        }
        if (Integer.valueOf(1).equals(reconcileLog.getChainExists())) {
            log.info("老支付充值链上数据已存在，跳过, orderId={}, txHash={}", orderNumber, txHash);
            return;
        }
        buildOldChainData(reconcileLog, coinId, orderNumber, amount, txHash, source, logObject.getBlockNumber().longValue(), logIndex);
        try {
            this.saveOrUpdate(reconcileLog);
        } catch (Exception e) {
            log.error("老支付充值事件落库失败，停止推进区块进度, orderId={}, txHash={}, logIndex={}",
                    orderNumber, txHash, logIndex, e);
            throw new RuntimeException("老支付充值事件落库失败", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveBizOrder(RechargeOrderLogDTO mqMessage) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String orderNumber = mqMessage.getOrderNumber();

            RechargeReconcileLog reconcileLog = this.getByOrderNumber(orderNumber);
            boolean isNewRecord = false;

            // 不存在则创建
            if (reconcileLog == null) {
                reconcileLog = new RechargeReconcileLog();
                isNewRecord = true;
            }
            reconcileLog.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
            reconcileLog.setReconcileRemark("业务数据已写入，等待对账");
            reconcileLog.setBizOrderNumber(orderNumber);
            reconcileLog.setCreateTime(now);
            // 填充业务数据
            reconcileLog.setBizExists(1);
            reconcileLog.setBizType(mqMessage.getType());
            reconcileLog.setBizHash(mqMessage.getHash() == null ? null : mqMessage.getHash().toLowerCase());
            reconcileLog.setBizUserId(mqMessage.getUserId());
            reconcileLog.setBizStatus(mqMessage.getStatus());
            reconcileLog.setBizAmount(mqMessage.getAmount());
            reconcileLog.setBizTokenAddress(mqMessage.getTokenAddress() == null ? null : mqMessage.getTokenAddress().toLowerCase());
            reconcileLog.setBizOrderTime(mqMessage.getOrderTime());
            reconcileLog.setUpdateTime(now);

            if (isNewRecord) {
                this.save(reconcileLog);
                log.info("新增充值业务订单成功, orderNumber={}", orderNumber);
            } else {
                this.updateById(reconcileLog);
                log.info("更新充值业务订单成功, orderNumber={}", orderNumber);
            }

        } catch (Exception e) {
            log.error("保存充值业务订单失败, orderNumber={}, error={}",
                    mqMessage == null ? null : mqMessage.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("保存充值业务订单失败", e);
        }
    }

    /**
     * 获取待对账的充值记录
     *
     * @return 充值记录列表
     */
    @Override
    public List<RechargeReconcileLog> queryPendingOrders() {
        LocalDateTime waitThreshold = LocalDateTime.now().minusMinutes(20);
        return this.lambdaQuery()
                .eq(RechargeReconcileLog::getReconcileStatus, ReconcileStatusEnum.PENDING.getCode())
                .and(wrapper -> wrapper.le(RechargeReconcileLog::getUpdateTime, waitThreshold)
                        .or(w -> w.eq(RechargeReconcileLog::getChainExists, 1)
                                .eq(RechargeReconcileLog::getBizExists, 1)))
                .orderByAsc(RechargeReconcileLog::getUpdateTime)
                .last("LIMIT 500").list();
    }

    @Override
    public void processMessage(String body) {
        RechargeReconcileLog source = JSONObject.parseObject(body, RechargeReconcileLog.class);
        // 查询是否存在
        RechargeReconcileLog existing = this.getByOrderNumber(source.getBizOrderNumber());
        if (existing == null) {
            // 不存在 → 新增
            this.save(source);
            log.info("新增充值记录，订单号: {}", source.getBizOrderNumber());
        } else {
            baseMapper.updateByOrderNumberSelective(source);
            log.info("更新充值记录，订单号: {}", source.getBizOrderNumber());
        }
    }


    /**
     * 检查链上事件是否已存在
     */
    private boolean existsByTxHashAndLogIndex(String txHash, Long logIndex) {
        return count(new LambdaQueryWrapper<RechargeReconcileLog>()
                .eq(RechargeReconcileLog::getChainTxHash, txHash)
                .eq(RechargeReconcileLog::getChainLogIndex, logIndex)) > 0;
    }

    /**
     * 构建老支付合约链上数据
     */
    private void buildOldChainData(RechargeReconcileLog log, BigInteger coinId, String orderId, BigInteger amount, String txHash, BigInteger source, Long blockNumber, Long logIndex) {
        BigDecimal humanAmount = new BigDecimal(amount)
                .divide(BigDecimal.TEN.pow(SysConfigConstants.TOKEN_DECIMALS), SysConfigConstants.TOKEN_DECIMALS, RoundingMode.DOWN)
                .stripTrailingZeros();
        log.setChainExists(1);
        log.setSource(source.intValue());
        log.setBizOrderNumber(orderId);
        log.setChainTokenAddress(getTokenAddress(coinId.intValue()));
        log.setChainReceiver("");
        log.setChainAmount(humanAmount);
        log.setChainTxHash(txHash);
        log.setChainBlockNumber(blockNumber);
        log.setChainLogIndex(logIndex);
        log.setChainTimestamp(new Date());
        log.setChainContractAddress(withdrawContractConfig.getRechargeContractAddress().toLowerCase());

        log.setReconcileStatus(ReconcileStatusEnum.PENDING.getCode());
        log.setReconcileRemark("老支付链上数据已写入，等待业务对账");
        log.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 根据老合约 coinId 获取币种地址
     */
    private String getTokenAddress(Integer coinId) {
        if (Integer.valueOf(1).equals(coinId)) {
            return "0x55d398326f99059ff775485246999027b3197955";
        }
        if (Integer.valueOf(2).equals(coinId)) {
            return "0x49f7c6d5af56ebf3c54817d0539db9f01a3dd5a1";
        }
        return "LP";
    }
}