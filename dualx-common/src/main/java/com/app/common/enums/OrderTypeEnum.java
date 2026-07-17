package com.app.common.enums;

import lombok.Getter;

/**
 * 订单类型枚举
 *
 * @author ll
 * @since 2025-10-20
 */
@Getter
public enum OrderTypeEnum {

    /**
     * 未知订单类型
     */
    UNKNOWN(0, "未知"),

    /**
     * 大额订单
     */
    LARGE(1, "大额"),

    /**
     * 小额订单
     */
    SMALL(2, "小额");

    /**
     * 类型码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String description;

    OrderTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 类型码
     * @return 枚举值，未找到返回 UNKNOWN
     */
    public static OrderTypeEnum fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (OrderTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 判断是否为有效订单类型
     *
     * @param code 类型码
     * @return true-有效，false-无效
     */
    public static boolean isValid(Integer code) {
        return fromCode(code) != UNKNOWN;
    }

    @Override
    public String toString() {
        return "OrderTypeEnum{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}