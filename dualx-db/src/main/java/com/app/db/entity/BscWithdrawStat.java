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
 * BSC出账统计快照表
 * </p>
 *
 * @author HayDen
 * @since 2026-05-12
 */
@Data
@TableName("bsc_withdraw_stat")
@Schema(name = "BscWithdrawStatSnapshot对象", description = "BSC出账统计快照表")
@Accessors(chain = true)
public class BscWithdrawStat implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description ="统计日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statDate;

    @Schema(description ="合约类型：ODIC/U")
    private String contractType;

    @Schema(description ="总出账笔数")
    private Long totalCount;

    @Schema(description ="总出账金额")
    private BigDecimal totalAmount;

    @Schema(description ="昨日出账笔数")
    private Long yesterdayCount;

    @Schema(description ="昨日出账金额")
    private BigDecimal yesterdayAmount;

    @Schema(description ="当日出账笔数")
    private Long todayCount;

    @Schema(description ="当日出账金额")
    private BigDecimal todayAmount;

    @Schema(description ="异常订单数")
    private Long abnormalCount;

    @Schema(description ="异常订单金额")
    private BigDecimal abnormalAmount;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
