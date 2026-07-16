package com.app.common.enums;

import com.app.common.model.EnumModel;

/**
 * 需要国际化的枚举类实现该接口
 */
public interface BaseEnum {

    int getEnumCode();

    default String label() {
        return null;
    }

    default String i18nKey() {
        return null;
    }

    default EnumModel toModel() {
        return new EnumModel(getEnumCode() + "", label());
    }
}
