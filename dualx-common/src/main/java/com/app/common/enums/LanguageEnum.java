package com.app.common.enums;

public enum LanguageEnum implements BaseEnum {

    zh_CN(0, "zh-CN"),
    zh_TW(1, "zh-TW"),
    en(2, "en"),
    ;
    private final int code;
    private final String remark;

    LanguageEnum(int code, String remark) {
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
        return "LanguageEnum." + getCode();
    }


    public static LanguageEnum valueOf(int enumCode) {
        for (LanguageEnum typeEnum : LanguageEnum.values()) {
            if (typeEnum.getEnumCode() == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }
}
