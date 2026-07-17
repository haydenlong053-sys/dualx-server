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
 *
 * </p>
 *
 * @author ll
 * @since 2025-10-20 10:35
 */
@Data
@TableName("sys_config")
@Schema(name = "SysConfig对象", description = "参数配置")
@Accessors(chain = true)
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description ="Config Id")
    @TableId(value = "config_id", type = IdType.AUTO)
    private Integer configId;

    @Schema(description ="参数名称")
    private String configName;

    @Schema(description ="参数键名")
    private String configKey;

    @Schema(description ="参数键值")
    private String configValue;

    @Schema(description ="系统内置（Y是 N否）")
    private String configType;

    @Schema(description ="创建者")
    private String createBy;

    @Schema(description ="创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description ="更新者")
    private String updateBy;

    @Schema(description ="更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description ="备注")
    private String remark;
}
