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
 * 支付对账每日统计表
 * </p>
 *
 * @author HayDen
 * @since 2026-05-18
 */
@Data
@TableName("payment_reconcile_stat")
@Schema(name = "PaymentReconcileStat对象", description = "支付对账每日统计表")
@Accessors(chain = true)
public class PaymentReconcileStat implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description ="统计日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate statDate;

    @Schema(description ="币种ID")
    private Integer coinId;

    @Schema(description ="币种名称")
    private String coinName;

    @Schema(description ="当日核对支付总笔数")
    private Integer totalCount;

    @Schema(description ="当日核对支付总金额")
    private BigDecimal totalAmount;

    @Schema(description ="对账成功订单数")
    private Integer successCount;

    @Schema(description ="对账成功金额")
    private BigDecimal successAmount;

    @Schema(description ="异常订单数")
    private Integer exceptionCount;

    @Schema(description ="异常订单金额")
    private BigDecimal exceptionAmount;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
