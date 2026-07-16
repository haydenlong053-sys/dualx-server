package com.app.web.config;

import com.app.common.annotation.SecureField;
import com.app.common.annotation.SecureRequest;
import com.app.common.annotation.Login;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Profile({"dev", "test", "uat", "test02", "test03"})
public class SwaggerConfig {

    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGN = "X-Sign";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes(HEADER_AUTHORIZATION, headerSecurityScheme(HEADER_AUTHORIZATION, "登录Token"))
                .addSecuritySchemes(HEADER_TIMESTAMP, headerSecurityScheme(HEADER_TIMESTAMP, "当前时间戳，秒或毫秒"))
                .addSecuritySchemes(HEADER_NONCE, headerSecurityScheme(HEADER_NONCE, "每次请求唯一随机字符串，5分钟内不能重复"))
                .addSecuritySchemes(HEADER_SIGN, headerSecurityScheme(HEADER_SIGN, "HMAC-SHA256签名")))
            .info(new Info()
                .title("INODE DOC")
                .description("I NODE接口文档")
                .version("1.0"));
    }

    @Bean
    public OperationCustomizer secureRequestOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!needSecure(handlerMethod)) {
                return operation;
            }
            operation.setSummary("[安全请求] " + operation.getSummary());
            operation.addTagsItem("安全请求");
            operation.addSecurityItem(new SecurityRequirement()
                .addList(HEADER_TIMESTAMP)
                .addList(HEADER_NONCE)
                .addList(HEADER_SIGN));
            addSecureHeader(operation, HEADER_TIMESTAMP, "当前时间戳，秒或毫秒", "1779783828032");
            addSecureHeader(operation, HEADER_NONCE, "每次请求唯一随机字符串，5分钟内不能重复", "d8f1b2c3a4e5f607");
            addSecureHeader(operation, HEADER_SIGN, "HMAC-SHA256签名", "按签名规则生成的十六进制字符串");

            List<String> secureFields = findSecureFields(handlerMethod);
            StringBuilder description = new StringBuilder();
            if (operation.getDescription() != null) {
                description.append(operation.getDescription()).append("\n\n");
            }
            description.append("【安全请求】该接口需要按 `docs/frontend-secure-request.md` 生成请求签名。");
            if (!secureFields.isEmpty()) {
                description.append("\n\n【RSA加密字段】")
                    .append(secureFields.stream().collect(Collectors.joining(", ")));
            }
            operation.setDescription(description.toString());
            return operation;
        };
    }

    @Bean
    public OperationCustomizer loginOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!needLogin(handlerMethod)) {
                return operation;
            }
            operation.addSecurityItem(new SecurityRequirement().addList(HEADER_AUTHORIZATION));
            addSecureHeader(operation, HEADER_AUTHORIZATION, "登录Token，登录接口返回的token", "登录后返回的token");
            return operation;
        };
    }

    private SecurityScheme headerSecurityScheme(String name, String description) {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name(name)
            .description(description);
    }

    @Bean
    public PropertyCustomizer secureFieldPropertyCustomizer() {
        return (schema, annotatedType) -> {
            if (hasSecureFieldAnnotation(annotatedType)) {
                String description = schema.getDescription();
                schema.setDescription((description == null ? "" : description + " ") + "【RSA加密字段】");
            }
            return schema;
        };
    }

    private boolean needSecure(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(SecureRequest.class)
            || handlerMethod.getBeanType().isAnnotationPresent(SecureRequest.class);
    }

    private boolean needLogin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(Login.class)
            || handlerMethod.getBeanType().isAnnotationPresent(Login.class);
    }

    private void addSecureHeader(io.swagger.v3.oas.models.Operation operation,
                                 String name,
                                 String description,
                                 String example) {
        if (!CollectionUtils.isEmpty(operation.getParameters())
            && operation.getParameters().stream().anyMatch(item -> name.equals(item.getName()))) {
            return;
        }
        operation.addParametersItem(new Parameter()
            .in("header")
            .name(name)
            .required(true)
            .description(description)
            .example(example)
            .schema(new StringSchema().example(example)));
    }

    private List<String> findSecureFields(HandlerMethod handlerMethod) {
        List<String> fields = new ArrayList<>();
        for (org.springframework.core.MethodParameter methodParameter : handlerMethod.getMethodParameters()) {
            if (!methodParameter.hasParameterAnnotation(RequestBody.class)) {
                continue;
            }
            Class<?> parameterType = methodParameter.getParameterType();
            Class<?> current = parameterType;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecureField.class)) {
                        fields.add(field.getName());
                    }
                }
                current = current.getSuperclass();
            }
        }
        return fields;
    }

    private boolean hasSecureFieldAnnotation(AnnotatedType annotatedType) {
        Annotation[] annotations = annotatedType.getCtxAnnotations();
        if (annotations == null) {
            return false;
        }
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == SecureField.class) {
                return true;
            }
        }
        return false;
    }
}
