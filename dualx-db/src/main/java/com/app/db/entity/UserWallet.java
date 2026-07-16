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
 * 资产
 * </p>
 *
 * @author HayDen
 * @since 2024-06-24
 */
@Data
@TableName(value = "user_wallet")
@Schema(name = "UserWallet对象", description = "资产")
public class UserWallet implements Serializable {

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

    @Schema(description = "币种")
    private String coinName;

    @Schema(description = "余额")
    private BigDecimal balance;

    @Schema(description = "冻结，余额的一部分")
    private BigDecimal frozen;

    @Schema(description = "版本")
    private Integer version;

}
