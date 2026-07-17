package com.app.web.service.impl;

import com.app.common.util.TelegramAlert;
import com.app.web.service.IRpcHealthCheckService;
import com.app.web.service.ISysConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;


import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
public class RpcHealthCheckServiceImpl implements IRpcHealthCheckService {

    @Resource
    private ISysConfigService sysConfigService;

    @Value("${contract.bsc-chain-rpc-urls:}")
    private List<String> bscChainRpcUrls;

    @Override
    public void checkAllRpcHealth() {
        if (bscChainRpcUrls == null || bscChainRpcUrls.isEmpty()) {
            log.debug("没有配置RPC节点，跳过健康检查");
            return;
        }
        log.debug("开始RPC节点健康检查, 节点数={}", bscChainRpcUrls.size());

        for (String url : bscChainRpcUrls) {
            String errorMessage = checkNodeHealthWithDetail(url);
            if (errorMessage != null) {
                String botToken = sysConfigService.getConfigValueByKey("BOT_TOKEN_ENV");
                String chatId = sysConfigService.getConfigValueByKey("CHAT_ID_ENV");
                String alertMsg = String.format("【RPC节点异常告警】\n节点: %s\n错误: %s\n时间: %s",
                        url, errorMessage, new java.util.Date());
                TelegramAlert.sendAlert("节点", alertMsg, botToken, chatId);
                log.warn("RPC节点不健康: {}, 错误: {}", url, errorMessage);
            }
        }
    }

    @Override
    public boolean checkNodeHealth(String url) {
        return checkNodeHealthWithDetail(url) == null;
    }

    /**
     * 检查节点健康状态，返回错误信息（null表示健康）
     */
    private String checkNodeHealthWithDetail(String url) {
        HttpService httpService = null;
        try {
            httpService = new HttpService(url);
            Web3j web3j = Web3j.build(httpService);
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            if (blockNumber != null && blockNumber.longValue() > 0) {
                return null;
            }
            return "返回区块号为null或0";
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            if (httpService != null) {
                try {
                    httpService.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}