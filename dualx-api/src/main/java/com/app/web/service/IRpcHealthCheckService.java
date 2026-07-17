package com.app.web.service;

public interface IRpcHealthCheckService {
    
    /**
     * 检查所有RPC节点健康状态
     */
    void checkAllRpcHealth();
    
    /**
     * 检查单个节点健康状态
     */
    boolean checkNodeHealth(String url);
    

}