package com.app.common.enums;

import com.app.common.model.EnumModel;

public enum AiRewardEnum implements BaseEnum {

    EQUAL_LEVEL_REWARD(1, "AI应用平级奖励"),
    REAL_TIME_REWARD(2, "AI应用实时奖励"),
    EXTRA_REWARD(3, "AI应用扩展奖励"),
    ;

    private final int code;
    private final String remark;

    AiRewardEnum(int code, String remark) {
        this.code = code;
        this.remark = remark;
    }

    public String getRemark() {
        return remark;
    }

    @Override
    public int getEnumCode() {
        return 0;
    }

    @Override
    public String i18nKey() {
        return "AiRewardEnum." + this.code;
    }

    @Override
    public EnumModel toModel() {
        return BaseEnum.super.toModel();
    }

    public static AiRewardEnum valueOf(int enumCode) {
        for (AiRewardEnum typeEnum : AiRewardEnum.values()) {
            if (typeEnum.code == enumCode) {
                return typeEnum;
            }
        }
        return null;
    }
}
