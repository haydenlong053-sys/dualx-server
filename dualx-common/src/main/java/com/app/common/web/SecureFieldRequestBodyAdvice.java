package com.app.common.web;

import com.app.common.annotation.SecureField;
import com.app.common.annotation.SecureRequest;
import com.app.common.config.SecureRequestProperties;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.RSACryptoUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.PrivateKey;

@ControllerAdvice
public class SecureFieldRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Resource
    private SecureRequestProperties properties;

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return properties.isEnabled()
            && (methodParameter.hasMethodAnnotation(SecureRequest.class)
            || methodParameter.getContainingClass().isAnnotationPresent(SecureRequest.class));
    }

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        decryptFields(body);
        return body;
    }

    private void decryptFields(Object body) {
        if (body == null) {
            return;
        }
        if (StringUtils.isBlank(properties.getRsaPrivateKey())) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
        try {
            PrivateKey privateKey = RSACryptoUtil.stringToPrivateKey(properties.getRsaPrivateKey());
            Class<?> clazz = body.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecureField.class) && field.getType() == String.class) {
                        field.setAccessible(true);
                        String encryptedValue = (String) field.get(body);
                        if (StringUtils.isNotBlank(encryptedValue)) {
                            field.set(body, RSACryptoUtil.decrypt(encryptedValue, privateKey));
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (DcException e) {
            throw e;
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }
}
