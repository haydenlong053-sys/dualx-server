package com.app.db.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-22 15:04
 */
@Data
public class BscWithdrawalLog implements Serializable {

    private static final long serialVersionUID = 1L;


    private Integer id;
    @Schema(description ="创建时间")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    private LocalDateTime updateTime;

    private String createBy;
    private String updateBy;

    @Schema(description ="标记删除，0 / 1")
    private Integer flag;

    @Schema(description ="来源 1:链桥   2:IM  3链桥老系统 4.美区 5 product")
    private Integer originType;

    @Schema(description ="来源方系统的用户ID")
    private String userId;

    @Schema(description ="币种 1:WX  2:WEBX")
    private Integer coinId;

    @Schema(description ="出账时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime billingTime;

    @Schema(description ="出账地址")
    private String fromAddress;

    @Schema(description ="入账地址")
    private String toAddress;

    @Schema(description ="链上交易hash")
    private String hash;

    @Schema(description ="提现状态 0:发起提现 1:正在提现 2:提现成功 3:提现失败")
    private Integer status;

    @Schema(description ="提现状态 0:未结束 1:签名完成")
    private Integer signFinished;

    @Schema(description ="一号签名进度 0:待签 1:签名中 2:签名成功 3:签名失败")
    private Integer signProgressOne;
    @Schema(description ="二号签名进度 0:待签 1:签名中 2:签名成功 3:签名失败")
    private Integer signProgressTwo;
    @Schema(description ="三号签名进度 0:待签 1:签名中 2:签名成功 3:签名失败")
    private Integer signProgressThree;
    @Schema(description ="四号签名进度 0:待签 1:签名中 2:签名成功 3:签名失败")
    private Integer signProgressFour;

    @Schema(description ="备注")
    private String remark;

    @Schema(description ="系统内部订单号")
    private String orderNumber;

    @Schema(description ="实际到账交易金额(扣除手续费的交易金额)")
    private BigDecimal amount;

    @Schema(description ="0=正常出账  1=闪兑出账")
    private Integer redemption;

    @Schema(description ="过期时间")
    private BigInteger deadline;

    @Schema(description ="提现状态 0:未判断 1:通过了大额审批 2.大额审批拒绝")
    private Integer largeAmountPassed;

    @Schema(description ="大额状态 0:未判断 1:是大额订单 2 小额订单")
    private Integer isLargeAmount;

    @Schema(description ="对账状态：0未对账 1成功 2异常")
    private Integer reconcileStatus;

}
