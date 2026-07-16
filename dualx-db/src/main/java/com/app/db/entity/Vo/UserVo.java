package com.app.db.entity.Vo;

import com.app.db.entity.UserWallet;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVo {

    private Integer id;

    @Schema(description = "用户头像")
    private String userImg;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "邀请码")
    private String shareCode;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "8位UID")
    private String uid;

    @Schema(description = "会员等级")
    private Integer level;

    @Schema(description = " 0：非会员  1：会员")
    private Integer levelStatus;;

    @Schema(description = "会员有效期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime userTime;

    @Schema(description = "充值地址")
    private String address;

    @Schema(description = "资产")
    private List<UserWallet> userWallets;
}
