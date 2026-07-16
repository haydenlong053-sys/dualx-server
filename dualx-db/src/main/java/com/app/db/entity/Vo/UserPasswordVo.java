package com.app.db.entity.Vo;

import com.app.db.entity.UserWallet;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class UserPasswordVo {

   private String account;

   private String password;

   private String base64Img;
}
