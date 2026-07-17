package com.app.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {
    
    @Bean("customWithdrawExecutor")
    public ThreadPoolTaskExecutor withdrawExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：通常保持活跃的线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数：峰值时允许的最大线程数
        executor.setMaxPoolSize(20);
        
        // 队列容量：当核心线程满时，任务放入队列
        executor.setQueueCapacity(200);
        
        // 线程空闲存活时间：60秒
        executor.setKeepAliveSeconds(60);
        
        // 线程名前缀
        executor.setThreadNamePrefix("withdraw-worker-");
        
        // 拒绝策略：由调用线程执行（保证任务不丢失）
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.initialize();
        return executor;
    }
}