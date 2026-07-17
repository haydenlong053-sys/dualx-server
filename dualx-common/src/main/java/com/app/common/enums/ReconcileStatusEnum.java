package com.app.common.enums;

import lombok.Getter;

/**
 * 对账状态枚举
 */
@Getter
public enum ReconcileStatusEnum {
    
    PENDING(0, "待对账"),
    SUCCESS(1, "对账成功"),
    FAIL(2, "对账异常");

    private final int code;
    private final String desc;

    ReconcileStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReconcileStatusEnum fromCode(int code) {
        for (ReconcileStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}