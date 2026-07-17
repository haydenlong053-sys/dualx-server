package com.app.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.lang.reflect.Proxy;
import java.util.List;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "contract")
@Data
public class Web3jConfig {

    private List<String> bscChainRpcUrls;

    /**
     * Web3j Bean 使用动态代理，每次调用从节点池获取健康实例
     */
    @Bean
    @Primary
    public Web3j web3j(Web3jNodeManager nodeManager) {
        return (Web3j) Proxy.newProxyInstance(
                Web3j.class.getClassLoader(),
                new Class[]{Web3j.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(nodeManager, args);
                    }
                    Web3j instance = nodeManager.getHealthyWeb3j();
                    return method.invoke(instance, args);
                }
        );
    }

    @Bean
    public Web3jNodeManager web3jNodeManager() {
        return new Web3jNodeManager(bscChainRpcUrls);
    }


    @Bean
    public Web3j web3jOther() {
        return Web3j.build(new HttpService("https://bsc-mainnet.core.chainstack.com/29da5183b096e8c0123c1034b1ad9565"));
    }
}