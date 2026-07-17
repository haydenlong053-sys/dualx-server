package com.app.web.task.pay;

import com.app.web.config.WithdrawContractConfig;
import com.app.web.service.IPaymentReconcileLogService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayOrderSyncTask {

    @Resource
    private IPaymentReconcileLogService paymentReconcileLogService;
    @Resource
    private WithdrawContractConfig withdrawContractConfig;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 同步链上扫描到的支付记录 发送给业务系统
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void syncPayEventToBiz() {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("支付订单同步任务仍在运行中，本次跳过");
            return;
        }
        try {
            if (withdrawContractConfig.getUseMq()) {
                paymentReconcileLogService.processSyncEvent();
            }
        } catch (Exception e) {
            log.error("支付订单同步任务异常", e);
        } finally {
            isRunning.set(false);
        }
    }


}