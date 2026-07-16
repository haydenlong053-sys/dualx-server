package com.app.web.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "com.app.task.enable", havingValue = "true")
public class SampleCronTask {

    @Scheduled(cron = "${com.app.task.cron.sampleCronTask}")
    public void execute() {
        log.info("示例定时任务-------------sample cron task executed");
    }
}
