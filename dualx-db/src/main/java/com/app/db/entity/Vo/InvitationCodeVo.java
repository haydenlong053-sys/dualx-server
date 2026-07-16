package com.app.db.entity.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 *
 * @author HayDen
 * @date 2024-01-09
 */
@Setter
@Getter
@Accessors(chain = true)
public class InvitationCodeVo
{
    private static final long serialVersionUID = 1L;

    /**
    * 邀请码
    */
    @Schema(description = "邀请码")
    private String invitationCode;

}
