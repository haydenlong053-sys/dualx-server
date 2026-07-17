package com.app.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 支付订单对账中心表
 * </p>
 *
 * @author HayDen
 * @since 2026-05-15
 */
@Data
@TableName("payment_reconcile_log")
@Schema(name = "PaymentReconcileLog对象", description = "支付订单对账中心表")
@Accessors(chain = true)
public class PaymentReconcileLog implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础字段 ====================

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description ="来源：1-新系统，2-美区（由前端传入) 3 IM 4 链桥PRODUCT区")
    private Integer source;

    @Schema(description ="标记删除，0:未删除 / 1:已删除")
    private Integer flag;

    @Schema(description ="同步MQ状态：0未处理 1发送中 2发送成功 3失败")
    private Integer sendStatus;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    // ==================== 业务数据（前缀：biz_） ====================

    @Schema(description ="【业务】订单号")
    private String bizOrderNumber;

    @Schema(description ="【业务】数据是否存在：0不存在 1存在")
    private Integer bizExists;

    @Schema(description ="【业务】订单类型")
    private String bizType;

    @Schema(description ="【业务】Hash")
    private String bizHash;

    @Schema(description ="【业务】用户ID")
    private String bizUserId;

    @Schema(description ="【业务】 2成功 3失败")
    private Integer bizStatus;

    @Schema(description ="【业务】积分金额")
    private BigDecimal bizPointAmount;

    @Schema(description ="【业务】代币金额")
    private BigDecimal bizTokenAmount;

    @Schema(description ="【业务】订单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime bizOrderTime;

    // ==================== 链上数据（前缀：chain_） ====================

    @Schema(description ="【链上】数据是否存在：0不存在 1存在")
    private Integer chainExists;

    @Schema(description ="【链上】用户地址")
    private String chainUserAddress;

    @Schema(description ="【链上】代币地址")
    private String chainTokenAddress;

    @Schema(description ="【链上】收款地址")
    private String chainReceiver;

    @Schema(description ="【链上】金额")
    private BigDecimal chainAmount;

    @Schema(description ="【链上】交易Hash")
    private String chainTxHash;

    @Schema(description ="【链上】区块号")
    private Long chainBlockNumber;

    @Schema(description ="【链上】日志索引")
    private Long chainLogIndex;

    @Schema(description ="【链上】时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date chainTimestamp;

    @Schema(description ="【链上】合约地址")
    private String chainContractAddress;

    // ==================== 对账结果（前缀：reconcile_） ====================

    @Schema(description ="【对账】金额是否一致：0否 1是")
    private Integer reconcileAmountMatch;

    @Schema(description ="【对账】Hash是否一致：0否 1是")
    private Integer reconcileHashMatch;

    @Schema(description ="【对账】状态是否一致：0否 1是")
    private Integer reconcileStatusMatch;

    @Schema(description ="【对账】状态：0待对账 1对账成功 2对账异常")
    private Integer reconcileStatus;

    @Schema(description ="【对账】结果类型：1完全一致 2业务有链上无 3链上有业务无 4金额不一致 5Hash不一致 6状态不一致")
    private Integer reconcileType;

    @Schema(description ="【对账】备注")
    private String reconcileRemark;

    @Schema(description ="【对账】时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reconcileTime;


}