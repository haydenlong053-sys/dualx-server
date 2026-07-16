package com.app.common.exception;

import com.app.common.enums.BaseEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DcException extends RuntimeException {

    private int code;

    private BaseEnum resultCodeEnum;

    private Map<String, ?> otherInfo;

    public DcException(BaseEnum codeEnum) {
        super(codeEnum.i18nKey());
        resultCodeEnum = codeEnum;
        code = codeEnum.getEnumCode();
    }

    public DcException(BaseEnum codeEnum, Map<String, ?> otherInfo) {
        super(codeEnum.i18nKey());
        resultCodeEnum = codeEnum;
        this.code = codeEnum.getEnumCode();
        this.otherInfo = otherInfo;
    }

    public int getCode() {
        return code;
    }

    public BaseEnum getResultCodeEnum() {
        return resultCodeEnum;
    }
}