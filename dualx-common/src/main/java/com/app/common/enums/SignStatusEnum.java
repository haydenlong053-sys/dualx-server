package com.app.common.enums;

/**
 * 签名状态枚举
 */
public enum SignStatusEnum {

    /**
     * 待签名
     */
    WAIT(0, "待签名"),

    /**
     * 签名中
     */
    PROCESSING(1, "签名中"),

    /**
     * 签名成功
     */
    SUCCESS(2, "签名成功"),

    /**
     * 签名失败
     */
    FAIL(3, "签名失败");

    private final int code;
    private final String desc;

    SignStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}