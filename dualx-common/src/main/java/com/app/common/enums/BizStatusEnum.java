package com.app.common.enums;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 业务状态枚举
 */
@Getter
@Schema(name  = "业务状态")
public enum BizStatusEnum {

    PENDING(0, "待处理"),

    PROCESSING(1, "处理中"),

    SUCCESS(2, "成功"),

    FAIL(3, "失败");

    private final int code;
    private final String desc;

    BizStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BizStatusEnum fromCode(int code) {
        for (BizStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}