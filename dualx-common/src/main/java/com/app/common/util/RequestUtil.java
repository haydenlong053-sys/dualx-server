package com.app.common.util;

import cn.dev33.satoken.stp.StpUtil;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

public final class RequestUtil {

    private static final Logger log = LoggerFactory.getLogger(RequestUtil.class);

    private static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static HttpServletResponse getResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }

    public static String getIp() {
        HttpServletRequest request = getRequest();
        String clientIp = request.getHeader("x-forwarded-for");
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp != null && clientIp.length() > 256 ? clientIp.substring(0, 255) : clientIp;
    }

    public static String getRequestURI() {
        HttpServletRequest request = getRequest();
        return request.getRequestURI();
    }

    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return request.getHeader(name);
    }

    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        return request.getParameter(name);
    }

    public static String getDevice() {
        HttpServletRequest request = getRequest();
        return request.getHeader("device");
    }

    public static String getAcceptLanguage() {
        String lang = getRequest().getHeader("Accept-Language");
        if (lang == null || lang.length() == 0) {
            return "en";
        }
        return lang;
    }

    public static String getAddress() {
        HttpServletRequest request = getRequest();
        String address = request.getHeader("address");
        if (address == null || address.isEmpty()) {
            address = request.getParameter("address");
        }
        if (address == null || address.isEmpty() || address.equals("null")) {
            return null;
        }
        return address.toLowerCase();
    }

    public static String getUserAgent() {
        String ua = getRequest().getHeader("User-Agent");
        return ua != null && ua.length() > 256 ? ua.substring(0, 255) : ua;
    }

    public static String getToken() {
        String token = getHeader("Authorization");
        if (token == null) {
            token = getParameter("token");
        }
        return token;
    }

    public static String getCurrentAccount() {
        if (!StpUtil.isLogin()) {
            throw new DcException(BaseResultCodeEnum.UN_AUTHORIZATION);
        }
        String id = StpUtil.getLoginIdAsString();
        if(StringUtils.isBlank(id)){
            throw new DcException(BaseResultCodeEnum.UN_AUTHORIZATION);
        }
        return id;
    }

    public static int getCurrentAccountId() {
        String id = getCurrentAccount();
        if (id != null) {
            return Integer.parseInt(id);
        }
        throw new DcException(BaseResultCodeEnum.UN_AUTHORIZATION, Collections.singletonMap("error", "Bad token"));
    }
}
