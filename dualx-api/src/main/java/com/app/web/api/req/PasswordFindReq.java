package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class PasswordFindReq {

    @Schema(description = "账号")
    @SecureField
    private String account;

    @Schema(description = "验证吗")
    @SecureField
    private String verification;

    @Schema(description = "1=邮箱 2=谷歌")
    private Integer status;
}
