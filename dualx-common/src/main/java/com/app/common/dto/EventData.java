package com.app.common.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 解析后的链上事件数据DTO
 */
@Data
@Builder
public class EventData  {
    
    /** 订单ID */
    private String orderId;
    
    /** 交易Hash */
    private String txHash;
    
    /** 用户地址 */
    private String userAddress;
    
    /** 金额（标准单位） */
    private BigDecimal humanAmount;
    
    /** 类型（0正常 1闪兑） */
    private Integer redemption;
    
    /** 执行地址 */
    private String executor;
    
    /** 时间戳 */
    private Date timestamp;
}