package com.app.web.service.impl;

import com.app.common.constants.RedisConstants;
import com.app.common.enums.CoinEnum;
import com.app.common.enums.WithdrawalStatusEnum;
import com.app.common.util.RedisUtil;
import com.app.db.entity.BscWithdrawalLog;
import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.*;

import com.beust.jcommander.internal.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WithdrawalExecuteServiceImpl implements IWithdrawalExecuteService {

    // ==================== 依赖注入 ====================
    @Resource
    private IBscWithdrawalLogService withdrawalLogService;

    @Resource
    private IWithdrawalProcessorService withdrawalProcessorService;

    @Resource
    private IWithdrawalStatusService withdrawalStatusService;

    @Resource
    private IWhitelistAddressService whitelistAddressService;

    /**
     * 自定义提现线程池
     */
    @Resource
    @Qualifier("customWithdrawExecutor")
    private ThreadPoolTaskExecutor withdrawExecutor;

    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    @Resource
    private IGasCheckService gasCheckService;

    // ==================== 任务执行状态锁 ====================
    private final AtomicBoolean isWithdrawRunning = new AtomicBoolean(false);

    @Override
    public void processWithdrawals() {
        log.debug("开始执行提现任务");
        if (!isWithdrawRunning.compareAndSet(false, true)) {
            log.info("提现任务仍在执行中，本次跳过");
            return;
        }
        try {
            if (!gasCheckService.hasEnoughGas(withdrawContractConfig.getExecuteWithdrawAddress()) ||
                    !gasCheckService.hasEnoughGas(withdrawContractConfig.getAddWhiteAddress())) {
                return;
            }
            withdrawalStatusService.markTimeoutOrders();
            withdrawalStatusService.handleChainConfirmingOrders();
            // 各区域独立提现（资金池不足则跳过）
            tryWithdraw("USDT_US", CoinEnum.USDT.getCoinId(), Lists.newArrayList(4),withdrawContractConfig.getUsContractWithdrawUsdt());
            tryWithdraw("ODIC_US", CoinEnum.OIDC.getCoinId(), Lists.newArrayList(4),withdrawContractConfig.getUsContractWithdrawOdic());

            tryWithdraw("USDT_PRODUCT", CoinEnum.USDT.getCoinId(), Lists.newArrayList(5),withdrawContractConfig.getProductContractWithdrawUsdt());
            tryWithdraw("ODIC_PRODUCT", CoinEnum.OIDC.getCoinId(), Lists.newArrayList(5),withdrawContractConfig.getProductContractWithdrawOdic());

            tryWithdraw("USDT", CoinEnum.USDT.getCoinId(), Lists.newArrayList(1, 2, 3), withdrawContractConfig.getContractWithdrawUsdt());
            tryWithdraw("ODIC", CoinEnum.OIDC.getCoinId(), Lists.newArrayList(1, 2, 3), withdrawContractConfig.getContractWithdrawOdic());
        } catch (Exception e) {
            log.error("提现任务执行异常", e);
        } finally {
            isWithdrawRunning.set(false);
        }
    }

    /**
     * 尝试提现（资金池不足自动跳过，异常不影响其他区域）
     */
    private void tryWithdraw(String regionKey, Integer coinId, List<Integer> types, String contractAddress) {
        // 资金池余额不足，暂停提现
        if (RedisUtil.hasKey(RedisConstants.WITHDRAW_BALANCE_EXECUTE + regionKey)) {
            log.info("资金池余额不足，暂停提现 [{}]", regionKey);
            return;
        }
        List<BscWithdrawalLog> orders = withdrawalLogService.lambdaQuery()
                .eq(BscWithdrawalLog::getStatus, WithdrawalStatusEnum.STATUS_PENDING.getCode())
                .eq(BscWithdrawalLog::getFlag, 0)
                .eq(BscWithdrawalLog::getSignFinished, 1)
                .in(BscWithdrawalLog::getOriginType, types)
                .eq(BscWithdrawalLog::getCoinId, coinId)
                .orderByAsc(BscWithdrawalLog::getId)
                .last("LIMIT 300")
                .list();
        if (orders.isEmpty()) {
            return;
        }
        log.info("发现 {} 笔待处理的{}订单 [{}]", orders.size(), regionKey, regionKey);
        try {
            processWithWhitelist(orders, contractAddress, regionKey);
        } catch (Exception e) {
            log.error("{}提现失败 [{}]，已隔离不影响其他区域", regionKey, regionKey, e);
        }
    }

    private void processWithWhitelist(List<BscWithdrawalLog> orders, String contractAddress, String coinName) throws Exception {
        boolean whitelistSuccess = addAddressesToWhitelist(orders, contractAddress);
        if (!whitelistSuccess) {
            log.warn("{} 白名单添加失败，本次跳过", coinName);
            return;
        }
        processOrdersWithThreadPool(orders, coinName);
    }

    private boolean addAddressesToWhitelist(List<BscWithdrawalLog> orders, String contractAddress) throws Exception {
        List<String> addressList = orders.stream()
                .map(BscWithdrawalLog::getToAddress)
                .distinct()
                .collect(Collectors.toList());
        return whitelistAddressService.filterAndAddWhitelist(addressList, contractAddress);
    }

    private void processOrdersWithThreadPool(List<BscWithdrawalLog> orders, String coinName) {
        if (orders.isEmpty()) {
            return;
        }
        // 按地址分组
        Map<String, List<BscWithdrawalLog>> ordersByAddress = orders.stream()
                .collect(Collectors.groupingBy(BscWithdrawalLog::getToAddress));
        List<List<BscWithdrawalLog>> addressOrderList = new ArrayList<>(ordersByAddress.values());
        // 线程数 = min(地址数, 最大线程数)，但不能少于1
        int threadCount = Math.min(addressOrderList.size(), 20);
        threadCount = Math.max(threadCount, 1);

        log.info("{}提现：订单总数={}, 地址数={}, 使用线程数={}", coinName, orders.size(), addressOrderList.size(), threadCount);

        processAddressOrdersConcurrently(addressOrderList, coinName, threadCount);
    }

    /**
     * 按地址并发处理（同一地址的订单串行）
     */
    private void processAddressOrdersConcurrently(List<List<BscWithdrawalLog>> addressOrdersList,
                                                  String coinName, int threadCount) {
        CountDownLatch latch = new CountDownLatch(addressOrdersList.size());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // 限制同时执行的线程数
        Semaphore semaphore = new Semaphore(threadCount);

        for (List<BscWithdrawalLog> addressOrders : addressOrdersList) {
            withdrawExecutor.submit(() -> {
                try {
                    semaphore.acquire();
                    String address = addressOrders.get(0).getToAddress();
                    log.info("开始处理地址 {}, 订单数 {}", address, addressOrders.size());
                    // 🔥 同一地址的订单串行处理
                    for (BscWithdrawalLog order : addressOrders) {
                        try {
                            boolean success = withdrawalProcessorService.process(order);
                            if (success) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                            }
                            // 同一地址的订单之间稍作延迟，避免 nonce 问题
                            Thread.sleep(500);
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            log.error("处理订单异常, orderId={}, address={}", order.getId(), address, e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("线程被中断");
                } finally {
                    semaphore.release();
                    latch.countDown();
                }
            });
        }
        try {
            boolean completed = latch.await(10, TimeUnit.MINUTES);
            long elapsed = System.currentTimeMillis() - startTime;
            if (completed) {
                log.info("{}并发完成：总数={}, 成功={}, 失败={}, 线程数={}, 耗时={}ms",
                        coinName,
                        addressOrdersList.stream().mapToInt(List::size).sum(),
                        successCount.get(), failCount.get(), threadCount, elapsed);
            } else {
                log.warn("{}并发超时", coinName);
            }
        } catch (InterruptedException e) {
            log.error("等待任务完成被中断", e);
            Thread.currentThread().interrupt();
        }
    }
}