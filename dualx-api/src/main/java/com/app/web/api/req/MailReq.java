package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class MailReq {

    @Schema(description = "绑定和修改的时候传  邮箱")
    @SecureField
    private String mail;

    @Schema(description = "1=绑定  3=修改 5=提币  7=转账 9=修改登录密码 11=用户注册 13=修改账号 15=找回登录密码 17=商家支付")
    private Integer status;
}
