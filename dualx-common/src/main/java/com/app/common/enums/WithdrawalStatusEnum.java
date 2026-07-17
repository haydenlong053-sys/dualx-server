package com.app.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 提现状态枚举
 *
 * 0 - 发起提现 / 待执行
 * 1 - 正在提现
 * 2 - 提现成功
 * 3 - 提现失败
 * 4 - 提现超时
 */
@Getter
@AllArgsConstructor
public enum WithdrawalStatusEnum {

    STATUS_PENDING(0, "发起提现 / 待执行"),
    STATUS_PROCESSING(1, "正在提现"),
    STATUS_SUCCESS(2, "提现成功"),
    STATUS_FAIL(3, "提现失败"),
    STATUS_TIMEOUT(4, "提现超时"),
    SYSTEM_ERROR(5, "系统错误需要人工介入"),
    STATUS_CHAIN_CONFIRMING(6, "链上确认中");

    private final int code;
    private final String desc;

    public static WithdrawalStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WithdrawalStatusEnum item : values()) {
            if (item.code == code) {
                return item;
            }
        }
        return null;
    }
}