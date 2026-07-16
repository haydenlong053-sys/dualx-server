package com.app.common.util;

import com.app.common.enums.BaseEnum;
import com.app.common.exception.DcException;

public class AssertUtil {

    public static void notNull(Object expression, BaseEnum message) {
        if (expression == null) {
            throw new DcException(message);
        }
    }

    public static void isTrue(boolean expression, BaseEnum message) {
        if (!expression) {
            throw new DcException(message);
        }
    }
}
