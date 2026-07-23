package com.app.web.service.impl;

import com.app.common.constants.RedisConstants;
import com.app.common.constants.SysConfigConstants;
import com.app.common.util.RedisUtil;
import com.app.web.config.Web3jNodeManager;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IChainEventScanService;
import com.app.web.service.IPaymentReconcileLogService;
import com.app.web.service.IRechargeReconcileLogService;
import com.app.web.service.IWithdrawReconcileLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;


import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ChainEventScanServiceImpl implements IChainEventScanService {

    @Resource
    private Web3jNodeManager web3jNodeManager;

    @Resource
    private IWithdrawReconcileLogService withdrawReconcileLogService;

    @Resource
    private IPaymentReconcileLogService paymentReconcileLogService;

    @Resource
    private IRechargeReconcileLogService rechargeReconcileLogService;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean startupBackScanned = new AtomicBoolean(false);

    private static final Event WITHDRAW_EXECUTED_EVENT = new Event(
            "WithdrawExecuted",
            Arrays.asList(
                    new TypeReference<Uint256>(true) {
                    },
                    new TypeReference<Address>(true) {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint8>() {
                    },
                    new TypeReference<Bytes32>() {
                    },
                    new TypeReference<Address>() {
                    },
                    new TypeReference<Uint256>() {
                    }
            )
    );

    private static final Event PAY_SUCCESS_EVENT = new Event(
            "PaySuccess",
            Arrays.asList(
                    new TypeReference<Utf8String>() {
                    },
                    new TypeReference<Address>(true) {
                    },
                    new TypeReference<Address>(true) {
                    },
                    new TypeReference<Address>(true) {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {  // 新增：source 字段
                    }
            )
    );

    private static final Event OLD_PAYMENT_EVENT = new Event(
            "PaymentMade",
            Arrays.asList(
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {  // 新增：source 字段
                    }
            )
    );

    // 预计算事件签名
    private static final String WITHDRAW_EXECUTED_SIGNATURE = EventEncoder.encode(WITHDRAW_EXECUTED_EVENT);
    private static final String PAY_SUCCESS_SIGNATURE = EventEncoder.encode(PAY_SUCCESS_EVENT);
    private static final String OLD_PAYMENT_SIGNATURE = EventEncoder.encode(OLD_PAYMENT_EVENT);

    @Override
    public void scanChainEvents() {
        if (!running.compareAndSet(false, true)) {
            log.info("链上事件扫描任务仍在执行中，本次跳过");
            return;
        }
        try {
            doScan();
        } catch (Exception e) {
            log.error("链上事件扫描异常", e);
        } finally {
            running.set(false);
        }
    }

    private void doScan() throws Exception {
        Web3j fixedWeb3j = web3jNodeManager.getHealthyWeb3j();
        BigInteger latestBlock = fixedWeb3j.ethBlockNumber().send().getBlockNumber();
        RedisUtil.setEx(RedisConstants.REDIS_KEY_LATEST_BLOCK, latestBlock.toString(), SysConfigConstants.REDIS_ONE_WEEK_SECONDS);

        BigInteger safeLatestBlock = latestBlock.subtract(BigInteger.valueOf(30));
        if (safeLatestBlock.compareTo(BigInteger.ZERO) <= 0) {
            log.info("安全区块异常，跳过扫描, latestBlock={}, safeLatestBlock={}",
                    latestBlock, safeLatestBlock);
            return;
        }

        BigInteger scannedBlock = getScannedBlockFromRedis();

        if (startupBackScanned.compareAndSet(false, true)) {
            BigInteger rollbackBlock = scannedBlock.subtract(new BigInteger("60"));
            log.info("服务启动回扫区块, oldScannedBlock={}, rollbackBlock={}, backBlocks={}", scannedBlock, rollbackBlock, 60);
            scannedBlock = rollbackBlock;
        }

        int times = 0;
        while (safeLatestBlock.subtract(scannedBlock).longValue() > 0 && times++ < 60) {
            BigInteger offset = calculateOffset(safeLatestBlock, scannedBlock);
            BigInteger fromBlock = scannedBlock.add(BigInteger.ONE);
            BigInteger toBlock = scannedBlock.add(offset);
            if (toBlock.compareTo(safeLatestBlock) > 0) {
                toBlock = safeLatestBlock;
            }
            if (!blockHeaderExists(fixedWeb3j, toBlock)) {
                log.warn("当前RPC节点还没有目标区块头，本轮停止扫描，不推进Redis, fromBlock={}, toBlock={}", fromBlock, toBlock);
                break;
            }
            boolean success = false;
            try {
                scanAllEvents(fixedWeb3j, fromBlock, toBlock);
                success = true;
            } catch (Exception e) {
                log.error("本轮区块扫描失败，不推进Redis，等待下次重试, fromBlock={}, toBlock={}", fromBlock, toBlock, e);
            }
            if (!success) {
                break;
            }
            scannedBlock = toBlock;
            RedisUtil.setEx(RedisConstants.REDIS_KEY_SCANNED_BLOCK, scannedBlock.toString(), SysConfigConstants.REDIS_ONE_WEEK_SECONDS);
        }
    }

    private boolean blockHeaderExists(Web3j web3j, BigInteger blockNumber) {
        try {
            return web3j.ethGetBlockByNumber(
                    new DefaultBlockParameterNumber(blockNumber),
                    false
            ).send().getBlock() != null;
        } catch (Exception e) {
            log.warn("检查区块头失败, blockNumber={}, error={}",
                    blockNumber, e.getMessage());
            return false;
        }
    }

    /**
     * 一次RPC调用扫描所有合约的所有关心的事件
     */
    private void scanAllEvents(Web3j web3j, BigInteger fromBlock, BigInteger toBlock) throws Exception {
        List<String> allContractAddresses = new ArrayList<>();
        Map<String, String> withdrawAddressToType = new HashMap<>();

        // 提现合约
        if (StringUtils.isNotBlank(withdrawContractConfig.getContractWithdrawOdic())) {
            allContractAddresses.add(withdrawContractConfig.getContractWithdrawOdic());
            withdrawAddressToType.put(withdrawContractConfig.getContractWithdrawOdic().toLowerCase(), "DUALX");
        }
        if (StringUtils.isNotBlank(withdrawContractConfig.getContractWithdrawUsdt())) {
            allContractAddresses.add(withdrawContractConfig.getContractWithdrawUsdt());
            withdrawAddressToType.put(withdrawContractConfig.getContractWithdrawUsdt().toLowerCase(), "U");
        }

        if (StringUtils.isNotBlank(withdrawContractConfig.getUsContractWithdrawOdic())) {
            allContractAddresses.add(withdrawContractConfig.getUsContractWithdrawOdic());
            withdrawAddressToType.put(withdrawContractConfig.getUsContractWithdrawOdic().toLowerCase(), "DUALX");
        }
        if (StringUtils.isNotBlank(withdrawContractConfig.getUsContractWithdrawUsdt())) {
            allContractAddresses.add(withdrawContractConfig.getUsContractWithdrawUsdt());
            withdrawAddressToType.put(withdrawContractConfig.getUsContractWithdrawUsdt().toLowerCase(), "U");
        }

        if (StringUtils.isNotBlank(withdrawContractConfig.getProductContractWithdrawOdic())) {
            allContractAddresses.add(withdrawContractConfig.getProductContractWithdrawOdic());
            withdrawAddressToType.put(withdrawContractConfig.getProductContractWithdrawOdic().toLowerCase(), "DUALX");
        }
        if (StringUtils.isNotBlank(withdrawContractConfig.getProductContractWithdrawUsdt())) {
            allContractAddresses.add(withdrawContractConfig.getProductContractWithdrawUsdt());
            withdrawAddressToType.put(withdrawContractConfig.getProductContractWithdrawUsdt().toLowerCase(), "U");
        }


        // 支付合约
        if (StringUtils.isNotBlank(withdrawContractConfig.getPaymentContractAddress())) {
            allContractAddresses.add(withdrawContractConfig.getPaymentContractAddress());
        }


        // 老支付合约（充值）
        if (StringUtils.isNotBlank(withdrawContractConfig.getRechargeContractAddress())) {
            allContractAddresses.add(withdrawContractConfig.getRechargeContractAddress());
        }
        //PRODUCT区的充值合约
        if (StringUtils.isNotBlank(withdrawContractConfig.getProductRechargeContractAddress())) {
            allContractAddresses.add(withdrawContractConfig.getProductRechargeContractAddress());
        }

        if (allContractAddresses.isEmpty()) {
            log.warn("所有合约地址均为空，跳过扫描");
            return;
        }

        log.debug("开始扫描区块范围 [{}, {}], 合约地址数量: {}",
                fromBlock, toBlock, allContractAddresses.size());

        EthFilter filter = new EthFilter(
                new DefaultBlockParameterNumber(fromBlock),
                new DefaultBlockParameterNumber(toBlock),
                allContractAddresses
        );

        filter.addOptionalTopics(
                WITHDRAW_EXECUTED_SIGNATURE,
                PAY_SUCCESS_SIGNATURE,
                OLD_PAYMENT_SIGNATURE
        );

        EthLog ethLog = web3j.ethGetLogs(filter).send();
        if (ethLog.hasError()) {
            throw new RuntimeException("ethGetLogs 查询事件失败, contractAddresses=" + allContractAddresses
                    + ", fromBlock=" + fromBlock + ", toBlock=" + toBlock
                    + ", error=" + ethLog.getError().getMessage());
        }

        List<EthLog.LogResult> logs = ethLog.getLogs();
        if (logs == null || logs.isEmpty()) {
            log.debug("本区块范围无关心的事件, fromBlock={}, toBlock={}", fromBlock, toBlock);
            return;
        }

        log.debug("获取到 {} 条关心的事件日志，开始分发处理", logs.size());

        for (EthLog.LogResult logResult : logs) {
            EthLog.LogObject logObject = (EthLog.LogObject) logResult.get();
            String contractAddress = logObject.getAddress();
            List<String> topics = logObject.getTopics();

            if (topics == null || topics.isEmpty()) {
                continue;
            }

            String eventSignature = topics.get(0);

            try {
                if (WITHDRAW_EXECUTED_SIGNATURE.equalsIgnoreCase(eventSignature)) {
                    String contractType = withdrawAddressToType.get(contractAddress.toLowerCase());
                    if (contractType != null) {
                        withdrawReconcileLogService.saveWithdrawExecutedEvent(logObject, contractAddress, contractType, WITHDRAW_EXECUTED_EVENT);
                    } else {
                        log.warn("未知的提现合约地址: {}, 跳过处理", contractAddress);
                    }
                } else if (PAY_SUCCESS_SIGNATURE.equalsIgnoreCase(eventSignature)) {
                    List<String> paymentList = new ArrayList<>();
                    paymentList.add(withdrawContractConfig.getPaymentContractAddress().toLowerCase());
                    if (paymentList.contains(contractAddress.toLowerCase())) {
                        paymentReconcileLogService.savePaySuccessEvent(logObject, PAY_SUCCESS_EVENT);
                    } else {
                        log.warn("支付事件来自非支付合约地址: {}, 跳过处理", contractAddress);
                    }
                } else if (OLD_PAYMENT_SIGNATURE.equalsIgnoreCase(eventSignature)) {
                    List<String> rechargeList = new ArrayList<>();
                    rechargeList.add(withdrawContractConfig.getRechargeContractAddress().toLowerCase());
                    rechargeList.add(withdrawContractConfig.getProductRechargeContractAddress().toLowerCase());
                    if (rechargeList.contains(contractAddress.toLowerCase())) {
                        rechargeReconcileLogService.saveRechargeSuccess(logObject, OLD_PAYMENT_EVENT);
                    } else {
                        log.warn("老支付事件来自非老支付合约地址: {}, 跳过处理", contractAddress);
                    }
                } else {
                    log.debug("忽略不相关事件, contractAddress={}, eventSignature={}", contractAddress, eventSignature);
                }
            } catch (Exception e) {
                log.error("事件处理失败，停止本轮扫描，避免区块进度被推进, contractAddress={}, eventSignature={}, txHash={}, logIndex={}",
                        contractAddress, eventSignature, logObject.getTransactionHash(), logObject.getLogIndex(), e);
                throw e;
            }
        }
    }

    private BigInteger calculateOffset(BigInteger latestBlock, BigInteger scannedBlock) {
        long gap = latestBlock.longValue() - scannedBlock.longValue();
        if (gap > 10000) {
            return BigInteger.valueOf(500);
        } else if (gap > 1000) {
            return BigInteger.valueOf(100);
        } else if (gap > 100) {
            return BigInteger.valueOf(50);
        } else if (gap > 10) {
            return BigInteger.valueOf(9);
        } else if (gap > 4) {
            return BigInteger.valueOf(3);
        } else {
            return BigInteger.ONE;
        }
    }

    private BigInteger getScannedBlockFromRedis() {
        String value = RedisUtil.get(RedisConstants.REDIS_KEY_SCANNED_BLOCK);
        if (StringUtils.isBlank(value)) {
            BigInteger initBlock = BigInteger.valueOf(withdrawContractConfig.getChainEventStartBlock());
            RedisUtil.setEx(RedisConstants.REDIS_KEY_SCANNED_BLOCK, initBlock.toString(), SysConfigConstants.REDIS_ONE_WEEK_SECONDS);
            log.info("初始化链上事件扫描区块, scannedBlock={}", initBlock);
            return initBlock;
        }
        return new BigInteger(value);
    }
}