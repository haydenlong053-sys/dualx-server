package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AccountSignInReq {

    @NotBlank
    @Schema(description = "设备窜")
    @SecureField
    private String account;

    @NotBlank
    @Schema(description = "MD5加密以后给我")
    @SecureField
    private String password;

    @NotNull
    @Schema(description = "链类型：1=Bitcoin 2=Ethereum 3=Polygon 4=BNB Chain 5=Arbitrum One")
    @Min(1)
    @Max(5)
    private Integer chainType;
}
