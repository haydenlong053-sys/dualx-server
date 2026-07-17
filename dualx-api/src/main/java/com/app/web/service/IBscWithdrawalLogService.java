package com.app.web.service;

import com.app.common.enums.WithdrawalStatusEnum;
import com.app.db.entity.BscWithdrawalLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-22 15:58
 */
public interface IBscWithdrawalLogService extends IService<BscWithdrawalLog> {

    /**
     * 根据订单号查询提现状态和哈希值
     *
     * @param orderId 订单号
     * @return 包含hash和status的Map
     */
    Map<String, String> getWithdrawalStatusByOrderId(String orderId);

    /**
     * 订单类型判断任务
     */
    void classifyOrderByRequiredSignatures();

    /**
     * 获取 超时订单
     *
     * @param timeoutTime 时间阈值
     * @param limit       数量
     * @return 订单列表
     */
    List<BscWithdrawalLog> getDeadlineOrder(LocalDateTime timeoutTime, int limit);

    /**
     * 修改订单状态
     *
     * @param id         订单主键
     * @param statusEnum 状态值
     * @param remark     备注
     * @return 是否成功
     */
    boolean markStatus(Integer id, WithdrawalStatusEnum statusEnum, String remark);

    /**
     * 判断订单走哪个合约
     *
     * @param order 订单
     * @return 合约地址
     */
    String getContractAddress(BscWithdrawalLog order);
}
