package com.app.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Login {

    //该接口是否加密
    boolean encrypted() default false;

    //AES解密密钥，在encrypted为true时有效，若不指定，则从当前token中获取
    String key() default "";

    //权限
    String[] permits() default {};
}
