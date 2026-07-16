package com.app.db.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.experimental.Accessors;

/**
 * <p>
 * Banner
 * </p>
 *
 * @author Auto
 * @since 2026-05-26
 */
@Data
@Schema(name = "Banner对象", description = "Banner")
@Accessors(chain = true)
public class Banner implements Serializable {

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

    @Schema(description = "标题")
    private String title;

    @Schema(description = "语言")
    private String lang;

    @Schema(description = "图片链接")
    private String imageUrl;

    @Schema(description = "0隐藏，1显示")
    private Integer state;

    @Schema(description = "跳转链接")
    private String targetUrl;

}
