package com.app.common.enums;

import lombok.Getter;

/**
 * 对账类型枚举
 */
@Getter
public enum ReconcileTypeEnum {
    
    FULLY_MATCH(1, "完全一致"),
    TYPE_BIZ_HAS_CHAIN_NONE(2, "业务有链上无"),
    TYPE_CHAIN_HAS_BIZ_NONE(3, "链上有业务无"),
    TYPE_AMOUNT_DIFF(4, "金额不一致"),
    TYPE_HASH_DIFF(5, "Hash不一致"),
    TYPE_STATUS_DIFF(6, "状态不一致"),

    TYPE_USER_MISMATCH( 6,"用户不一致"),
    TYPE_REDEMPTION_MISMATCH(7,"提现方式不一致");

    private final int code;
    private final String desc;

    ReconcileTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReconcileTypeEnum fromCode(int code) {
        for (ReconcileTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}