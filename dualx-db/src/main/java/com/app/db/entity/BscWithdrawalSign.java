package com.app.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * <p>
 * 提现签名记录
 * </p>
 *
 * @author ll
 * @since 2026-04-15
 */
@Data
@TableName("bsc_withdrawal_sign")
@Schema(name = "BscWithdrawalSign对象", description = "提现签名记录")
@Accessors(chain = true)
public class BscWithdrawalSign implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private String createBy;
    private String updateBy;

    @Schema(description ="标记删除，0 / 1")
    private Integer flag;

    @Schema(description ="提现申请记录ID，关联 bsc_withdrawal_log.id")
    private Integer withdrawLogId;

    @Schema(description ="签名人地址")
    private String signerAddress;

    @Schema(description ="签名摘要")
    private String signDigest;

    @Schema(description ="签名结果")
    private String signature;

    @Schema(description ="签名状态 0:待签 1:签名中 2:签名成功 3:签名失败")
    private Integer signStatus;

    @Schema(description ="签名步骤 1:第一审核 2:第二审核 3:第三审核")
    private Integer signStep;

    @Schema(description ="签名服务器标识")
    private String signServer;

    @Schema(description ="备注")
    private String remark;

    @Schema(description ="签名时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signTime;

    @Schema(description ="合约订单号")
    private String orderId;

    @Schema(description ="签名过期时间戳")
    private BigInteger deadline;

    @Schema(description ="用户nonce")
    private BigInteger userNonce;

    @Schema(description ="业务来源标识")
    private BigInteger bizId;

    @Schema(description ="签名失败原因")
    private String failReason;
}