package com.app.web.service;

import com.app.db.entity.WhitelistAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 白名单地址表 服务类
 * </p>
 *
 * @author HayDen
 * @since 2026-04-24
 */
public interface IWhitelistAddressService extends IService<WhitelistAddress> {
    /**
     * 根据地址列表过滤并新增白名单（数据库 + 链上）
     *
     * @param addressList 地址列表
     * @return true=处理成功，false=没有新增
     * @throws Exception 异常信息
     */
    boolean filterAndAddWhitelist(List<String> addressList,String contractAddress) throws Exception;

}
