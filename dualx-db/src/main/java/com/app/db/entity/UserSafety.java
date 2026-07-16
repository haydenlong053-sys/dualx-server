package com.app.db.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户安全中心
 * </p>
 *
 * @author HayDen
 * @since 2025-05-26
 */
@Data
@TableName(value = "user_safety")
@Schema(name = "UserSafety对象", description = "用户安全中心")
@Accessors(chain = true)
public class UserSafety implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "标记删除，0 / 1")
    private Integer flag;

    @Schema(description = "状态 1=正常 0=谷歌验证待审核  2=邮箱验证待审核  3=默认")
    private Integer status;

    @Schema(description = "邮箱")
    private String mail;

    @Schema(description = "谷歌验证器")
    private String google;

    @Schema(description = "登录密码")
    private String passwordLogin;

    @Schema(description = "支付密码")
    private String passwordPay;
}
