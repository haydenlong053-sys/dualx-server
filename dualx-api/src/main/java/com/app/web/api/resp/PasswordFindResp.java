package com.app.web.api.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author hayden
 */
@Data
@Accessors(chain = true)
public class PasswordFindResp {

    @Schema(description = "是否设置谷歌验证")
    private boolean google;


    @Schema(description = "是否设置邮箱")
    private boolean mail;

    @Schema(description = "邮箱账户")
    private String mailAccount;
}
