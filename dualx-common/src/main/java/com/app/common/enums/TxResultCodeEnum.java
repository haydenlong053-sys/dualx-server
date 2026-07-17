package com.app.common.enums;

import lombok.Getter;

@Getter
public enum TxResultCodeEnum implements BaseEnum {

    // ========== 交易查询相关 ==========
    TX_NOT_FOUND(3001, "交易未确认或不存在"),
    TX_EXECUTION_FAILED(3002, "交易执行失败"),
    TX_EVENT_NOT_FOUND(3003, "未找到对应事件"),
    TX_QUERY_EXCEPTION(3004, "查询交易异常，请稍后再试"),

    // ========== 交易发送相关 ==========
    TX_SEND_FAILED(3010, "交易发送失败"),
    TX_GAS_TOO_LOW(3011, "Gas费用低于网络最低要求"),
    TX_NONCE_ERROR(3012, "Nonce错误，请重试"),
    TX_TIMEOUT(3013, "交易超时未确认"),

    // ========== 合约调用相关 ==========
    CONTRACT_REVERT(3020, "合约执行回滚"),
    INVALID_SOURCE(3021, "无效的来源"),
    INVALID_PAYMENT_TYPE(3022, "无效的支付类型"),
    TOKEN_NOT_ALLOWED(3023, "代币未在白名单中"),
    AMOUNT_TOO_SMALL(3024, "金额低于最低限额"),
    ORDER_ALREADY_EXISTS(3025, "订单已存在"),
    ORDER_NOT_EXISTS(3026, "订单不存在"),
    CONTRACT_PAUSED(3027, "合约已暂停"),
    ZERO_ADDRESS(3028, "地址不能为零地址"),
    ZERO_AMOUNT(3029, "金额不能为零"),

    // ========== 权限相关 ==========
    NOT_ADMIN(3030, "非管理员操作"),
    IP_NOT_ALLOWED(3031, "IP不在白名单中"),

    // ========== 提现相关 ==========
    WITHDRAWAL_FAILED(3040, "提现失败"),
    WITHDRAWAL_CHANNEL_CLOSED(3041, "提现通道已关闭"),
    WITHDRAWAL_MIN_LIMIT(3042, "低于最低提现限额"),
    INSUFFICIENT_BALANCE(3043, "余额不足"),
    WITHDRAWAL_ADDRESS_ERROR(3044, "提现地址错误"),

    // ========== 支付相关 ==========
    PAYMENT_FAILED(3050, "支付失败"),
    PAYMENT_ALREADY_SUCCESS(3051, "该订单已支付成功"),
    PAYMENT_EXPIRED(3052, "支付已过期"),

    // ========== 通用 ==========
    UNKNOWN_ERROR(3999, "交易未知错误");

    private final int code;
    private final String remark;

    TxResultCodeEnum(int code, String remark) {
        this.code = code;
        this.remark = remark;
    }

    @Override
    public int getEnumCode() {
        return this.code;
    }

    @Override
    public String i18nKey() {
        return "TxResultCodeEnum." + this.code;
    }

    public static TxResultCodeEnum valueOf(int enumCode) {
        for (TxResultCodeEnum typeEnum : TxResultCodeEnum.values()) {
            if (typeEnum.code == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }
}