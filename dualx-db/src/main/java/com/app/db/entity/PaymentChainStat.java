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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 链上充值统计快照表
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Data
@TableName("payment_chain_stat")
@Schema(name ="PaymentChainStat对象", description = "链上充值统计快照表")
@Accessors(chain = true)
public class PaymentChainStat implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description ="统计日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statDate;

    @Schema(description ="代币地址")
    private String tokenAddress;

    @Schema(description ="总充值笔数")
    private Long totalCount;

    @Schema(description ="总充值金额")
    private BigDecimal totalAmount;

    @Schema(description ="昨日充值笔数")
    private Long yesterdayCount;

    @Schema(description ="昨日充值金额")
    private BigDecimal yesterdayAmount;

    @Schema(description ="当日充值笔数")
    private Long todayCount;

    @Schema(description ="当日充值金额")
    private BigDecimal todayAmount;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
