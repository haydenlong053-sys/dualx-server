package com.app.common.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 提现订单消息
 */
@Data
public class WithdrawOrderMessage {
    private String orderNumber;      // 系统内部订单号
    private Integer originType;      // 来源 1:链桥 2:IM 3.链桥老系统
    private String userId;           // 用户ID
    private Integer coinId;          // 币种
    private String toAddress;        // 入账地址
    private BigDecimal amount;       // 实际到账金额
    private Integer redemption;      // 出账类型 0正常 1闪兑
    private String hash;             // 业务系统Hash
    private Integer status;          // 提现状态 2:成功 3:失败
}