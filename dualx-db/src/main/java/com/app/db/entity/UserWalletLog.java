package com.app.db.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * <p>
 * 资产流水
 * </p>
 *
 * @author HayDen
 * @since 2024-06-24
 */
@Data
@TableName(value = "user_wallet_log")
@Schema(name = "UserWalletLog对象", description = "资产流水")
public class UserWalletLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "标记删除，0 / 1")
    private Integer flag;

    @Schema(description = "")
    private Integer memberId;

    @Schema(description = "")
    private Integer walletId;

    @Schema(description = "1余额，2冻结")
    private Integer type;

    @Schema(description = "币种")
    private String coinName;

    @Schema(description = "")
    private Integer opType;

    @Schema(description = "流水类型")
    private String opRemark;

    @Schema(description = "")
    private BigDecimal opValue;

    @Schema(description = "")
    private BigDecimal opBefore;

    @Schema(description = "")
    private BigDecimal opAfter;

    @Schema(description = "额外的备注")
    private String extRemark;

}




