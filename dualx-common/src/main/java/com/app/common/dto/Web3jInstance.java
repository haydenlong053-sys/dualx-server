package com.app.common.dto;

import lombok.Data;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

/**
 * Web3j实例包装类
 */
@Data
public class Web3jInstance {
    private final String instanceId;
    private final String url;
    private final Web3j web3j;
    private final HttpService httpService;
    private volatile boolean healthy;
    private volatile BigInteger latestBlock;
    private final long createTime;

    public Web3jInstance(String id, String url, Web3j web3j, HttpService http, boolean healthy, BigInteger block) {
        this.instanceId = id;
        this.url = url;
        this.web3j = web3j;
        this.httpService = http;
        this.healthy = healthy;
        this.latestBlock = block;
        this.createTime = System.currentTimeMillis();
    }

    public void close() {
        if (httpService != null) {
            try {
                httpService.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}