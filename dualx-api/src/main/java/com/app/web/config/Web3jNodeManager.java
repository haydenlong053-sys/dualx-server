package com.app.web.config;

import com.app.common.dto.Web3jInstance;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import jakarta.annotation.PreDestroy;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Web3jNodeManager {

    private static final int POOL_SIZE = 6;
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 60;

    // P0修复：超时配置（毫秒）
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int HEALTH_CHECK_TIMEOUT_MS = 5000;

    // P1修复：健康检查重试次数
    private static final int MAX_HEALTH_CHECK_RETRIES = 3;

    private final List<Web3jInstance> instances = new CopyOnWriteArrayList<>();
    private final List<String> rpcUrls;
    private final AtomicInteger roundRobin = new AtomicInteger(0);

    // P1修复：分离线程池职责
    private final ScheduledExecutorService healthCheckScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService instanceExecutor = Executors.newFixedThreadPool(2);

    private final CountDownLatch initLatch = new CountDownLatch(1);
    private volatile boolean initialized = false;

    // P0修复：池大小控制锁
    private final Object poolLock = new Object();

    public Web3jNodeManager(List<String> rpcUrls) {
        this.rpcUrls = rpcUrls;
        initializePool();
        startHealthCheckScheduler();
    }

    /**
     * 创建带超时的 OkHttpClient
     */
    private OkHttpClient createOkHttpClient(int connectTimeoutMs, int readTimeoutMs) {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .writeTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    private void startHealthCheckScheduler() {
        healthCheckScheduler.scheduleAtFixedRate(() -> {
            try {
                healthCheck();
            } catch (Exception e) {
                log.error("健康检查任务执行异常", e);
            }
        }, HEALTH_CHECK_INTERVAL_SECONDS, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void initializePool() {
        log.info("初始化实例池, 目标={}", POOL_SIZE);
        long start = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(POOL_SIZE);
        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < POOL_SIZE; i++) {
            instanceExecutor.submit(() -> {
                if (createInstance()) {
                    success.incrementAndGet();
                }
                latch.countDown();
            });
        }

        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("实例池初始化超时，只创建了{}/{}个实例", success.get(), POOL_SIZE);
            }
            log.info("实例池初始化完成, 成功={}, 耗时={}ms", success.get(), System.currentTimeMillis() - start);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        initialized = true;
        initLatch.countDown();

        if (instances.isEmpty()) {
            log.error("没有成功创建任何实例，请检查RPC节点配置");
        }
        printStats();
    }

    private boolean createInstance() {
        synchronized (poolLock) {
            if (instances.size() >= POOL_SIZE) {
                log.debug("实例池已满，跳过创建");
                return false;
            }
        }

        String url = selectUrl();

        int maxPerUrl = Math.max(1, POOL_SIZE / rpcUrls.size());
        long sameUrlCount = 0;
        for (Web3jInstance inst : instances) {
            if (inst.getUrl().equals(url)) {
                sameUrlCount++;
            }
        }

        if (sameUrlCount >= maxPerUrl && rpcUrls.size() > 1) {
            Map<String, Long> urlCount = new HashMap<>();
            for (Web3jInstance inst : instances) {
                urlCount.merge(inst.getUrl(), 1L, Long::sum);
            }
            String minUrl = rpcUrls.get(0);
            long minCount = urlCount.getOrDefault(minUrl, 0L);
            for (String u : rpcUrls) {
                long c = urlCount.getOrDefault(u, 0L);
                if (c < minCount) {
                    minCount = c;
                    minUrl = u;
                }
            }
            if (!minUrl.equals(url)) {
                url = minUrl;
            }
        }

        final String finalUrl = url;

        for (int retry = 0; retry < 3; retry++) {
            HttpService http = null;
            try {
                // P0修复：使用 OkHttpClient 设置超时
                OkHttpClient client = createOkHttpClient(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                http = new HttpService(finalUrl, client, false);
                Web3j web3j = Web3j.build(http);
                BigInteger block = web3j.ethBlockNumber().send().getBlockNumber();

                if (block != null) {
                    synchronized (poolLock) {
                        if (instances.size() >= POOL_SIZE) {
                            log.warn("实例池已满，关闭连接: {}", finalUrl);
                            http.close();
                            return false;
                        }
                        instances.add(new Web3jInstance(UUID.randomUUID().toString(), finalUrl, web3j, http, true, block));
                    }
                    log.info("创建实例成功, url={}, block={}, 池大小={}/{}", finalUrl, block, instances.size(), POOL_SIZE);
                    return true;
                }
            } catch (Exception e) {
                log.warn("创建实例失败, url={}, retry={}, error={}", finalUrl, retry, e.getMessage());
                if (http != null) {
                    try { http.close(); } catch (Exception ignored) {}
                }

                if (retry == 2) {
                    return false;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private String selectUrl() {
        if (rpcUrls.isEmpty()) {
            throw new RuntimeException("没有配置RPC节点");
        }
        if (rpcUrls.size() == 1) {
            return rpcUrls.get(0);
        }

        Map<String, Long> count = new HashMap<>();
        for (Web3jInstance inst : instances) {
            count.merge(inst.getUrl(), 1L, Long::sum);
        }

        String bestUrl = rpcUrls.get(0);
        long minCount = count.getOrDefault(bestUrl, 0L);
        for (String u : rpcUrls) {
            long c = count.getOrDefault(u, 0L);
            if (c < minCount) {
                minCount = c;
                bestUrl = u;
            }
        }
        return bestUrl;
    }

    private void healthCheck() {
        log.debug("开始主动健康检查, 当前实例数={}", instances.size());

        List<Web3jInstance> toRemove = new ArrayList<>();

        for (Web3jInstance instance : instances) {
            boolean isHealthy = false;
            for (int retry = 0; retry < MAX_HEALTH_CHECK_RETRIES; retry++) {
                isHealthy = checkInstanceHealth(instance);
                if (isHealthy) break;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!isHealthy) {
                log.warn("节点不健康, 准备剔除: {}", instance.getUrl());
                toRemove.add(instance);
            } else {
                instance.setHealthy(true);
            }
        }

        for (Web3jInstance instance : toRemove) {
            synchronized (poolLock) {
                instances.remove(instance);
            }
            instance.close();
            log.info("剔除不健康节点: {}", instance.getUrl());
        }

        if (instances.size() < POOL_SIZE) {
            int need = POOL_SIZE - instances.size();
            log.info("需要补充 {} 个实例", need);
            for (int i = 0; i < need; i++) {
                instanceExecutor.submit(this::createInstance);
            }
        }

        log.info("健康检查完成, 当前实例数={}, 健康数={}", instances.size(), getHealthyCount());
    }

    /**
     * P1修复：健康检查使用独立临时连接，带超时
     */
    private boolean checkInstanceHealth(Web3jInstance instance) {
        HttpService tempHttp = null;
        try {
            // P0修复：使用 OkHttpClient 设置超时
            OkHttpClient client = createOkHttpClient(HEALTH_CHECK_TIMEOUT_MS, HEALTH_CHECK_TIMEOUT_MS);
            tempHttp = new HttpService(instance.getUrl(), client, false);
            Web3j tempWeb3j = Web3j.build(tempHttp);
            BigInteger block = tempWeb3j.ethBlockNumber().send().getBlockNumber();
            if (block != null) {
                instance.setLatestBlock(block);
                return true;
            }
        } catch (Exception e) {
            log.debug("节点不健康: {}, error={}", instance.getUrl(), e.getMessage());
        } finally {
            if (tempHttp != null) {
                try { tempHttp.close(); } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public Web3j getHealthyWeb3j() {
        if (!initialized) {
            try {
                boolean completed = initLatch.await(10, TimeUnit.SECONDS);
                if (!completed) {
                    throw new RuntimeException("初始化超时");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待初始化被中断", e);
            }
        }
        if (instances.isEmpty()) {
            throw new RuntimeException("无可用实例");
        }

        List<Web3jInstance> healthyInstances = new ArrayList<>();
        for (Web3jInstance inst : instances) {
            if (inst.isHealthy()) {
                healthyInstances.add(inst);
            }
        }

        if (healthyInstances.isEmpty()) {
            log.warn("没有健康实例，触发紧急检查");
            healthCheck();
            for (Web3jInstance inst : instances) {
                if (inst.isHealthy()) {
                    healthyInstances.add(inst);
                }
            }
        }

        if (healthyInstances.isEmpty()) {
            log.warn("仍然没有健康实例，紧急创建临时实例");
            return createEmergencyInstance();
        }

        int idx = Math.abs(roundRobin.getAndIncrement() % healthyInstances.size());
        Web3jInstance selected = healthyInstances.get(idx);
        log.debug("轮询返回节点: {}", selected.getUrl());
        return selected.getWeb3j();
    }

    /**
     * P0修复：紧急实例连接泄漏修复
     */
    private Web3j createEmergencyInstance() {
        log.warn("紧急创建临时实例");
        for (String url : rpcUrls) {
            HttpService http = null;
            try {
                OkHttpClient client = createOkHttpClient(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                http = new HttpService(url, client, false);
                Web3j web3j = Web3j.build(http);
                web3j.ethBlockNumber().send();
                log.info("紧急创建临时实例成功: {}", url);
                return web3j;
            } catch (Exception e) {
                log.warn("紧急创建临时实例失败: {}", url);
                if (http != null) {
                    try { http.close(); } catch (Exception ignored) {}
                }
            }
        }
        throw new RuntimeException("无可用节点");
    }

    public void printStats() {
        log.info("========== 实例池统计 ==========");
        log.info("实例总数: {}/{}", instances.size(), POOL_SIZE);
        log.info("健康实例数: {}", getHealthyCount());
        for (Web3jInstance instance : instances) {
            String shortUrl = instance.getUrl().length() > 50 ?
                    instance.getUrl().substring(0, 50) + "..." :
                    instance.getUrl();
            log.info("  - {}: healthy={}", shortUrl, instance.isHealthy());
        }
        log.info("=================================");
    }

    public int getHealthyCount() {
        int count = 0;
        for (Web3jInstance inst : instances) {
            if (inst.isHealthy()) {
                count++;
            }
        }
        return count;
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭节点管理器");

        healthCheckScheduler.shutdown();
        instanceExecutor.shutdown();

        try {
            // 处理返回值
            boolean healthCheckTerminated = healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS);
            if (!healthCheckTerminated) {
                healthCheckScheduler.shutdownNow();
                log.warn("健康检查线程池未正常终止，强制关闭");
            }

            boolean instanceTerminated = instanceExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!instanceTerminated) {
                instanceExecutor.shutdownNow();
                log.warn("实例线程池未正常终止，强制关闭");
            }
        } catch (InterruptedException e) {
            healthCheckScheduler.shutdownNow();
            instanceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("关闭节点管理器时被中断");
        }

        for (Web3jInstance inst : instances) {
            inst.close();
        }
        instances.clear();
    }
}