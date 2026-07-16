package com.app.common.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BusinessLog {
    /**
     * 业务的名称,例如:"修改菜单"
     */
    String value() default "";

    boolean skipLogParam() default false;
}
