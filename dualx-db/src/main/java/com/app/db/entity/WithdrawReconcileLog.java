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
 * BSC 提现对账记录表
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Data
@TableName("withdraw_reconcile_log")
@Schema(name = "WithdrawReconcileLog对象", description = "BSC提现对账记录表")
@Accessors(chain = true)
public class WithdrawReconcileLog implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础字段 ====================

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description ="标记删除，0:未删除 / 1:已删除")
    private Integer flag;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    // ==================== 业务数据（前缀：biz_） ====================

    @Schema(description ="【业务】系统内部订单号")
    private String bizOrderNumber;

    @Schema(description ="【业务】业务订单是否存在：0未检查 1存在 2不存在")
    private Integer bizOrderExists;

    @Schema(description ="【业务】来源 1:链桥 2:IM 3:链桥老系统")
    private Integer bizOriginType;

    @Schema(description ="【业务】来源方系统的用户ID")
    private String bizUserId;

    @Schema(description ="【业务】币种 1:WX 2:WEBX")
    private Integer bizCoinId;

    @Schema(description ="【业务】提现收款地址")
    private String bizToAddress;

    @Schema(description ="【业务】提现状态 2:提现成功 3:提现失败")
    private Integer bizStatus;

    @Schema(description ="【业务】实际到账金额(扣除手续费)")
    private BigDecimal bizAmount;

    @Schema(description ="【业务】出账类型：0正常出账 1闪兑出账")
    private Integer bizRedemption;

    @Schema(description ="【业务】业务系统交易Hash（关联用）")
    private String bizHash;

    // ==================== 链上数据（前缀：chain_） ====================

    @Schema(description ="【链上】链上订单是否存在：0未检查 1存在 2不存在")
    private Integer chainOrderExists;

    @Schema(description ="【链上】收款地址")
    private String chainUserAddress;

    @Schema(description ="【链上】提现金额")
    private BigDecimal chainAmount;

    @Schema(description ="【链上】业务类型：0正常提币 1闪兑提币")
    private Integer chainRedemption;

    @Schema(description ="【链上】交易Hash")
    private String chainTxHash;

    @Schema(description ="【链上】区块号")
    private Long chainBlockNumber;

    @Schema(description ="【链上】日志索引")
    private Long chainLogIndex;

    @Schema(description ="【链上】执行地址")
    private String chainExecutor;

    @Schema(description ="【链上】事件时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date chainTimestamp;

    @Schema(description ="【链上】合约类型：DUALX/U")
    private String chainContractType;

    @Schema(description ="【链上】合约地址")
    private String chainContractAddress;

    // ==================== 对账结果（前缀：reconcile_） ====================
    @Schema(description ="【对账】提现状态是否一致：0否 1是")
    private Integer reconcileStatusMatch;

    @Schema(description ="【对账】金额是否一致：0否 1是")
    private Integer reconcileAmountMatch;

    @Schema(description ="【对账】用户地址是否一致：0否 1是")
    private Integer reconcileUserMatch;

    @Schema(description ="【对账】交易Hash是否一致：0否 1是")
    private Integer reconcileHashMatch;

    @Schema(description ="【对账】类型是否一致：0否 1是")
    private Integer reconcileRedemptionMatch;

    @Schema(description ="【对账】状态：0待对账 1对账成功 2对账异常")
    private Integer reconcileStatus;

    @Schema(description ="【对账】结果类型：1完全一致 2业务有链上无 3链上有业务无 4金额不一致 5Hash不一致 6用户不一致 7类型不一致")
    private Integer reconcileType;

    @Schema(description ="【对账】备注")
    private String reconcileRemark;

    @Schema(description ="【对账】时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reconcileTime;

}