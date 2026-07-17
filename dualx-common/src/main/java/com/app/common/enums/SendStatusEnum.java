package com.app.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 发送状态枚举
 */
@Getter
@Schema(name = "发送状态")
public enum SendStatusEnum {
    

    PENDING(0, "未发送"),
    

    PROCESSING(1, "发送中"),
    

    SUCCESS(2, "已发送"),
    

    FAILED(3, "发送失败");

    private final int code;
    private final String desc;

    SendStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SendStatusEnum fromCode(int code) {
        for (SendStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}