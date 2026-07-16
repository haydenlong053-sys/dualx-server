package com.app.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author HayDen
 * @since 2024-06-24
 */
@Data
@TableName(value = "user")
@Schema(name = "User对象", description = "用户")
@Accessors(chain = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "标记删除，0 / 1")
    private Integer flag;

    @Schema(description = "用户头像")
    private String userImg;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "账号类型，1地址，2邮箱")
    private Integer accountType;

    @Schema(description = "推荐人ID")
    private Integer referId;

    @Schema(description = "上面的人，团队，含自己")
    private String teamAllPid;

    @Schema(description = "邀请码")
    private String shareCode;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "8位UID")
    private String uid;

    @Schema(description = "0=无等级 1=会员 2=会员")
    private Integer level;

    @Schema(description = "直推人数")
    private Integer shareNum;

    @Schema(description = "团队人数，含自己")
    private Integer teamNum;

    @Schema(description = "链类型：1=Bitcoin2= Ethereum3= Polygon4= BNB Chain  5=Arbitrum One")
    private Integer chainType;

    @Schema(description = "下级人数")
    private Integer numberOfSubordinates;

    @Schema(description = "会员有效期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime userTime;

    @Schema(description = "0：非会员 1：会员")
    private Integer levelStatus;

    @Schema(description = "用户团队业绩（含自己）")
    private BigDecimal teamAmount;

    @Schema(description = "用户个人业绩")
    private BigDecimal userAmount;

    @Schema(description = "累计充值")
    private BigDecimal recharge;

}
