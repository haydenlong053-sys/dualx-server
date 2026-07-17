package com.app.web.service.impl;

import com.app.db.entity.WhitelistAddress;
import com.app.db.mapper.WhitelistAddressMapper;
import com.app.web.service.IWhitelistAddressService;
import com.app.web.service.IWithdrawContractKmsService;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 白名单地址表 服务实现类
 * </p>
 *
 * @author HayDen
 * @since 2026-04-24
 */
@Service
@Slf4j
public class WhitelistAddressServiceImpl extends ServiceImpl<WhitelistAddressMapper, WhitelistAddress> implements IWhitelistAddressService {

    @Resource
    private IWithdrawContractKmsService withdrawContractKmsService;

    /**
     * 根据地址列表过滤并新增白名单（数据库 + 链上）
     * <p>
     * 作用：
     * 1. 传入地址列表
     * 2. 自动去重
     * 3. 过滤数据库已存在地址
     * 4. 新地址批量入库
     * 5. 新地址批量上链白名单
     *
     * @param addressList 地址列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean filterAndAddWhitelist(List<String> addressList, String contractAddress) throws Exception {
        if (CollectionUtils.isEmpty(addressList)) {
            return true;
        }
        //1. 清洗 + 去重
        Set<String> uniqueAddresses = addressList.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (uniqueAddresses.isEmpty()) {
            return true;
        }
        log.info("待处理白名单地址数量={}", uniqueAddresses.size());
        //2. 查询数据库已存在地址
        Set<String> existingAddresses = this.lambdaQuery()
                .in(WhitelistAddress::getAddress, uniqueAddresses)
                .eq(WhitelistAddress::getContract, contractAddress)
                .list()
                .stream()
                .map(item -> item.getAddress().toLowerCase())
                .collect(Collectors.toSet());

        //3. 过滤新地址
        List<String> newAddresses = uniqueAddresses.stream()
                .filter(address -> !existingAddresses.contains(address))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(newAddresses)) {
            log.info("所有地址已存在白名单，无需新增");
            return true;
        }
        log.info("需要新增白名单地址数量={}", newAddresses.size());
        //4. 批量入库
        List<WhitelistAddress> saveList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String address : newAddresses) {
            WhitelistAddress entity = new WhitelistAddress();
            entity.setAddress(address);
            entity.setContract(contractAddress);
            entity.setStatus(1);
            entity.setRemark("提现自动加入白名单");
            entity.setCreatedTime(now);
            saveList.add(entity);
        }
        boolean saved = this.saveBatch(saveList);
        if (!saved) {
            return false;
        }
        TransactionReceipt receipt = withdrawContractKmsService.batchSetAllowedUsers(newAddresses, true, contractAddress);
        if (receipt == null || !receipt.isStatusOK()) {
            return false;
        }
        log.info("白名单地址上链成功，txHash={}, 数量={}",
                receipt.getTransactionHash(),
                newAddresses.size());
        return true;
    }

}
