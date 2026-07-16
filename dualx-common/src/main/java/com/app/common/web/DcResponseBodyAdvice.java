package com.app.common.web;

import com.app.common.model.BaseResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.reactivestreams.Publisher;

/**
 * 此类处理结果统一处理
 */
@Configuration
@RestControllerAdvice
public class DcResponseBodyAdvice implements ResponseBodyAdvice {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return returnType.getDeclaringClass().getPackage().getName().startsWith("com.app")
                && !Publisher.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof BaseResult) {
            return body;
        }
        if (body instanceof String) {
            // 处理返回类型为String的情况
            BaseResult<String> baseResult = new BaseResult<>((String) body);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return new ObjectMapper().writeValueAsString(baseResult);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing JSON", e);
            }
        }
        return new BaseResult<>(body);
    }

}
