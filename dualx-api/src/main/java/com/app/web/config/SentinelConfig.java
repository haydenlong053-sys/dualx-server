package com.app.web.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import com.alibaba.csp.sentinel.adapter.web.common.UrlCleaner;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.app.common.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SentinelConfig {

    @Bean
    public BlockExceptionHandler sentinelBlockExceptionHandler() {
        return (request, response, resourceName, e) -> writeBlockedResponse(response, resourceName, e);
    }

    @Bean
    public UrlCleaner sentinelUrlCleaner() {
        return originUrl -> {
            if (originUrl == null) {
                return null;
            }
            return originUrl.replaceAll("/\\d+(?=/|$)", "/{id}");
        };
    }

    @Bean
    public RequestOriginParser sentinelRequestOriginParser() {
        return request -> {
            String ip = IpUtils.getIpAddr(request);
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                return "unknown";
            }
            return ip.split(",")[0].trim();
        };
    }

    private void writeBlockedResponse(HttpServletResponse response,
                                      String resourceName,
                                      BlockException e) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{"
            + "\"code\":429,"
            + "\"msg\":\"" + blockMessage(e) + "\","
            + "\"timeMillis\":" + System.currentTimeMillis() + ","
            + "\"data\":{\"resource\":\"" + escapeJson(resourceName) + "\"}"
            + "}");
    }

    private String blockMessage(BlockException e) {
        if (e instanceof FlowException) {
            return "请求过于频繁，请稍后再试";
        }
        if (e instanceof DegradeException) {
            return "服务暂时不可用，请稍后再试";
        }
        if (e instanceof ParamFlowException) {
            return "热点参数访问过于频繁，请稍后再试";
        }
        if (e instanceof SystemBlockException) {
            return "系统繁忙，请稍后再试";
        }
        if (e instanceof AuthorityException) {
            return "请求来源无权限";
        }
        return "请求被限流，请稍后再试";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
