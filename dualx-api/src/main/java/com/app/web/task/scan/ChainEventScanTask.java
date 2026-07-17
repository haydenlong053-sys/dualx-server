package com.app.web.task.scan;

import com.app.web.service.IChainEventScanService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;



@Slf4j
@Component
public class ChainEventScanTask {

    @Resource
    private IChainEventScanService chainEventScanService;

    /**
     * 扫块任务 会扫描 支付 充值 提（U/ODIC）合约并保存对账数据
     */
    @Scheduled(initialDelay = 15 * 1000, fixedDelay = 15 * 1000)
    public void scanChainEvents() {
        chainEventScanService.scanChainEvents();
    }
}