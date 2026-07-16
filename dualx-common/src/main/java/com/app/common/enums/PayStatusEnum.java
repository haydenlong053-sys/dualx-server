package com.app.common.enums;

public enum PayStatusEnum implements BaseEnum {

    PENDING_PAYMENT(1, "待支付"),
    PAYMENT_CONFIRMING(2, "支付确认中"),
    CANCELED(3, "已取消"),
    MANUAL_CONFIRMATION(4, "等待人工确认"),
    COMPLETED(5, "已完成"),
    ;
    private final int code;
    private final String remark;

    PayStatusEnum(int code, String remark) {
        this.code = code;
        this.remark = remark;
    }

    public int getCode() {
        return code;
    }

    public String getRemark() {
        return remark;
    }

    @Override
    public int getEnumCode() {
        return this.code;
    }

    @Override
    public String i18nKey() {
        return "PayStatusEnum." + getCode();
    }


    public static PayStatusEnum valueOf(int enumCode) {
        for (PayStatusEnum typeEnum : PayStatusEnum.values()) {
            if (typeEnum.getEnumCode() == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }
}
