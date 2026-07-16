package com.app.db.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
public abstract class BaseEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Timestamp createTime;

    //private String createBy;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Timestamp updateTime;

    //private String updateBy;

    @TableLogic(value = "1", delval = "0")
    private Integer flag;
}
