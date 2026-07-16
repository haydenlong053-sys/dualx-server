package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class PasswordPayReq {

    @Schema(description = "原密码，支付第一次设置可以不传")
    @SecureField
    private String fromPassword;

    @NotBlank
    @Schema(description = "新密码")
    @SecureField
    private String toPassword;

    @Schema(description = "谷歌谷歌验证吗")
    @SecureField
    private String verification;

    @Schema(description = "1=邮箱 2=谷歌")
    private Integer status;

    @Schema(description = "1=修改登录密码 2=修改支付密码")
    private Integer type;
}
