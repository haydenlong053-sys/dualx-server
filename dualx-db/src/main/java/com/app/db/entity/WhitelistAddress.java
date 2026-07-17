package com.app.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 白名单地址表
 * </p>
 *
 * @author HayDen
 * @since 2026-04-24
 */
@Data
@TableName("whitelist_address")
@Schema(name =  "WhitelistAddress对象", description = "白名单地址表")
@Accessors(chain = true)
public class WhitelistAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description ="ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description ="白名单地址（如IP、钱包地址、邮箱等）")
    private String address;

    @Schema(description ="合约")
    private String contract;

    @Schema(description ="地址类型：1-IP地址，2-钱包地址，3-邮箱，4-域名")
    private Integer addressType;

    @Schema(description ="状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description ="备注信息")
    private String remark;

    @Schema(description ="过期时间（NULL表示永久有效）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description ="创建人")
    private String createdBy;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description ="更新人")
    private String updatedBy;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

}
