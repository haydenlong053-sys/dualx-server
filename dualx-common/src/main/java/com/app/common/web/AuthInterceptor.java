package com.app.common.web;

import cn.dev33.satoken.stp.StpUtil;
import com.app.common.annotation.Login;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.RedisUtil;
import com.app.common.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Log4j2
@Configuration
@Component(value = "authInterceptor")
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        Login login = ((HandlerMethod) handler).getMethodAnnotation(Login.class);
        if (login == null) {
            return true;
        }
        if (login.encrypted()) {
            return true;
        }
        if (!StpUtil.isLogin()) {
            throw new DcException(BaseResultCodeEnum.UN_AUTHORIZATION);
        }
        String path = request.getServletPath();
        String method = request.getMethod();

        if (method.equalsIgnoreCase("POST")) {
            String str = request.getServletPath();
            if (str.contains("api/lucky") ||
                str.contains("api/order") ||
                str.contains("api/wallet") ||
                str.contains("api/member") ||
                str.contains("api/home/category") ||
                str.contains("api/v2") ||
                str.contains("api/virtualOrder")
            ) {
                //throw new IllegalArgumentException("系统维护，暂时关闭");
            }

            if (str.contains("/api/cart")) {
                return true;
            }
        }
        String account = StpUtil.getLoginIdAsString();
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }
        String cacheKey = String.format("smt:apiLimit:%s-%s-%s", account, method, path);
        String cache = RedisUtil.get(cacheKey);
        if (cache != null) {
            throw new DcException(BaseResultCodeEnum.IT_S_TOO_FREQUENT);
        }
        RedisUtil.setEx(cacheKey, RequestUtil.getIp(), 2);
        return true;
    }
}
