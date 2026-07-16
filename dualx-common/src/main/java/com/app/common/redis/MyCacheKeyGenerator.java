package com.app.common.redis;

import com.app.common.util.Md5Util;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * 缓存key自定义生成规则，
 */
public class MyCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String className = target.getClass().getName();
        String methodName = method.getName();
        StringBuilder builder = new StringBuilder();

        if (params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                builder.append(params[i] != null ? params[i] : "NULL").append("&");
            }
        }
        return String.format("%s:%s:%s", className, methodName, Md5Util.md5(builder.toString()));
    }
}
