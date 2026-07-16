package com.app.common.enums;

public enum WalletLogTypeEnum implements BaseEnum {

    DEFAULT(0, "系统操作"),
    ADMIN_ADD(1, "系统充值"),
    ADMIN_SUB(2, "系统扣除"),
    RECHARGE(3, "链上充值"),

    COMMODITY_REWARDS(4, "商品奖励"),
    TEAM_DIVIDEND_REWARDS(5, "团队分红奖励"),

//    PRE_DEDUCTIBLE_FOR_ORDERING(6, "商品下单预扣"),
//    ORDER_OF_GOODS(7, "商品下单"),

    PRODUCT_PAYMENT(8, "商品付款"),

//    CANCEL_AN_ORDER(9, "取消订单"),
    PRE_DEDUCTIBLE_FOR_WITHDRAWAL(10, "提现预扣"),
    SUCCESSFUL_WITHDRAWAL(11, "提现成功"),
    SUCCESSFUL_WITHDRAWAL_FEE(12, "提现手续费"),

    TRANSFER(14, "转账"),
    TOP_UP(15, "充值"),

    FAILED_TO_WITHDRAW_MONEY(21, "提币失败"),

    TRANSFER_FEE(39, "转账手续费"),
    FINANCE_INCOME(40, "金融产品收益"),
    ;

    private final int code;
    private final String remark;

    WalletLogTypeEnum(int code, String remark) {
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
        return "WalletLogTypeEnum." + getCode();
    }

    @Override
    public String label() {
        return getRemark();
    }

    public static WalletLogTypeEnum valueOf(int enumCode) {
        for (WalletLogTypeEnum typeEnum : WalletLogTypeEnum.values()) {
            if (typeEnum.getEnumCode() == enumCode) {
                return typeEnum;
            }
        }
        return WalletLogTypeEnum.DEFAULT;
    }
}
