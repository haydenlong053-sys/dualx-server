package com.app.common.enums;

public enum CommodityTypeEnum implements BaseEnum {

    VIP(1, "会员包"),
    FINANCE(2, "金融产品"),
    ;
    private final int code;
    private final String remark;

    CommodityTypeEnum(int code, String remark) {
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
        return "CommodityTypeEnum." + getCode();
    }


    public static CommodityTypeEnum valueOf(int enumCode) {
        for (CommodityTypeEnum typeEnum : CommodityTypeEnum.values()) {
            if (typeEnum.getEnumCode() == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }
}
