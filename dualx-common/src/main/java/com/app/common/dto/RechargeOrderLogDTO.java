package com.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值业务订单同步DTO
 */
@Data
@Schema(name = "RechargeOrderLogDTO", description = "充值业务订单同步DTO")
public class RechargeOrderLogDTO {

    @Schema(description ="【业务】订单号")
    private String orderNumber;

    @Schema(description ="【业务】订单类型")
    private String type;

    @Schema(description ="【业务】Hash")
    private String hash;

    @Schema(description ="【业务】用户ID")
    private String userId;

    @Schema(description ="【业务】订单状态 2成功 3失败 ")
    private Integer status;

    @Schema(description ="【业务】充值金额")
    private BigDecimal amount;

    @Schema(description ="【业务】合约地址")
    private String tokenAddress;

    @Schema(description ="【业务】订单时间")
    private LocalDateTime orderTime;
}