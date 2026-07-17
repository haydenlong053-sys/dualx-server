package com.app.web.task.withdraw;

import com.app.web.service.IGasCheckService;
import com.app.web.service.IRpcHealthCheckService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Profile("prod")
public class ExecutorWithdrawTask {


    @Resource
    private IGasCheckService gasCheckService;

    @Resource
    private IRpcHealthCheckService rpcHealthCheckService;

    /**
     * gas 判任务 如果缺少gas或者usdt 代币 之类的会发telegram消息给客服
     */
    @Scheduled(fixedDelay = 30000)
    public void gas() {
        gasCheckService.gas();
    }

    /**
     * RPC节点健康检查
     * 每分钟执行一次，探测所有节点，将不健康的节点标记到Redis
     */
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 60 * 1000)
    public void checkRpcHealth() {
        rpcHealthCheckService.checkAllRpcHealth();
    }

}