package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class NewMailReq {

    @Schema(description = "验证码 修改的时候传，谷歌或者邮箱验证吗")
    @SecureField
    private String verified;

    @Schema(description = "新邮箱")
    @SecureField
    private String newMail;

    @Schema(description = "新邮箱验证码")
    @SecureField
    private String newVerified;

    @Schema(description = "1=绑定  3=修改")
    private Integer status;
}
