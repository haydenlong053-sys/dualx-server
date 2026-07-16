package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class AccountSignUpReq {

    @Schema(description = "account")
    @SecureField
    private String account;

    @Schema(description = "invitation code")
    private String shareCode;

    //@Schema("publicKey")
    //private String publicKey;

    @NotNull
    @Schema(description = "链类型：1=Bitcoin 2=Ethereum 3=Polygon 4=BNB Chain 5=Arbitrum One")
    @Min(1)
    @Max(5)
    private Integer chainType;

    //@NotBlank
//    private String emailCode;

    @Schema(description = "登陆密码")
    @SecureField
    private String password;

//    @NotNull
//    @Schema("注册类型：1=邮箱 2=自定义账号")
//    @Min(1)
//    @Max(2)
//    private Integer registerType;
}
