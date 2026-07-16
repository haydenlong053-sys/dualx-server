package com.app.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "com.app.task.enable", havingValue = "true")
public class TaskConfig implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(TaskConfig.class);

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(taskExecutor());
    }

    @Bean
    public ThreadPoolExecutor taskExecutor() {
        log.info("new ScheduledThreadPoolExecutor");
        return new ScheduledThreadPoolExecutor(10) {
            @Override
            protected void terminated() {
                super.terminated();
                log.warn("Task Executor terminated");
            }
        };
    }
}
