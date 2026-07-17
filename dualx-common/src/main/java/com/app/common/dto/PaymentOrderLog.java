package com.app.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Data
public class PaymentOrderLog implements Serializable {

    private static final long serialVersionUID = 1L;

   @Schema(description ="订单ID")
    private String orderNumber;

   @Schema(description ="订单类型 type 1、商品  2、开通信用分 3、开通商家")
    private String type;

   @Schema(description ="hash值")
    private String hash;

   @Schema(description ="userId")
    private String userId;

   @Schema(description ="3 失败 2：已完成/已开通")
    private Integer status;

   @Schema(description ="积分金额")
    private BigDecimal pointAmount;

   @Schema(description ="代币金额")
    private BigDecimal tokenAmount;

   @Schema(description ="订单创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;

   @Schema(description ="代币地址")
    private String tokenAddress;


}
