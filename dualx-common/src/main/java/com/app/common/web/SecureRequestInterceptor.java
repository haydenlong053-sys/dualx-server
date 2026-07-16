package com.app.common.web;

import com.app.common.annotation.SecureRequest;
import com.app.common.config.SecureRequestProperties;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Component
public class SecureRequestInterceptor implements HandlerInterceptor {

    private static final String NONCE_PREFIX = "SECURE_REQUEST_NONCE:";

    @Resource
    private SecureRequestProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled() || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!needSecure(handlerMethod)) {
            return true;
        }
        String timestamp = request.getHeader(properties.getTimestampHeader());
        String nonce = request.getHeader(properties.getNonceHeader());
        String sign = request.getHeader(properties.getSignHeader());
        if (StringUtils.isAnyBlank(timestamp, nonce, sign, properties.getSignSecret())) {
            log.warn("安全请求校验失败: 缺少必要参数, uri={}, timestampHeader={}, timestamp={}, nonceHeader={}, nonce={}, signHeader={}, sign={}, signSecretConfigured={}",
                request.getRequestURI(),
                properties.getTimestampHeader(),
                timestamp,
                properties.getNonceHeader(),
                nonce,
                properties.getSignHeader(),
                sign,
                StringUtils.isNotBlank(properties.getSignSecret()));
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        checkTimestamp(request, timestamp);
        checkNonce(request, nonce);

        String body = request instanceof CachedBodyHttpServletRequest cachedRequest
            ? cachedRequest.getCachedBody()
            : "";
        String bodyHash = sha256(body);
        String signText = request.getMethod().toUpperCase()
            + "\n" + request.getRequestURI()
            + "\n" + timestamp
            + "\n" + nonce
            + "\n" + bodyHash;
        String expectedSign = hmacSha256(signText, properties.getSignSecret());
        if (!expectedSign.equalsIgnoreCase(sign)) {
            log.warn("安全请求校验失败: 签名不匹配, uri={}, method={}, bodyHash={}, expectedSign={}, actualSign={}, signText={}",
                request.getRequestURI(),
                request.getMethod().toUpperCase(),
                bodyHash,
                expectedSign,
                sign,
                signText.replace("\n", "\\n"));
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        return true;
    }

    private boolean needSecure(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(SecureRequest.class)
            || handlerMethod.getBeanType().isAnnotationPresent(SecureRequest.class);
    }

    private void checkTimestamp(HttpServletRequest request, String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            if (String.valueOf(requestTime).length() == 10) {
                requestTime *= 1000;
            }
            long diffSeconds = Math.abs(Instant.now().toEpochMilli() - requestTime) / 1000;
            if (diffSeconds > properties.getTimestampWindowSeconds()) {
                log.warn("安全请求校验失败: 时间戳超出允许窗口, uri={}, timestamp={}, diffSeconds={}, windowSeconds={}",
                    request.getRequestURI(),
                    timestamp,
                    diffSeconds,
                    properties.getTimestampWindowSeconds());
                throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
            }
        } catch (NumberFormatException e) {
            log.warn("安全请求校验失败: 时间戳格式错误, uri={}, timestamp={}", request.getRequestURI(), timestamp);
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }

    private void checkNonce(HttpServletRequest request, String nonce) {
        String key = NONCE_PREFIX + nonce;
        if (!RedisUtil.tryLock(key, (int) properties.getNonceExpireSeconds())) {
            log.warn("安全请求校验失败: nonce重复或Redis锁定失败, uri={}, nonce={}, expireSeconds={}",
                request.getRequestURI(),
                nonce,
                properties.getNonceExpireSeconds());
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }

    private String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }
}
