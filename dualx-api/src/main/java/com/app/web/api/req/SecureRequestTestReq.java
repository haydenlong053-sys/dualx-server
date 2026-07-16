package com.app.web.api.req;

import com.app.common.annotation.SecureField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "安全请求测试参数")
public class SecureRequestTestReq {

    @SecureField
    @Schema(description = "测试账号，前端需要RSA加密")
    private String account;

    @SecureField
    @Schema(description = "测试密码，前端需要RSA加密")
    private String password;

    @Schema(description = "普通字段，不需要加密")
    private String remark;
}
