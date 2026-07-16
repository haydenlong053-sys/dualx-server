package com.app.web.api;

import com.app.common.config.SecureRequestProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
@Tag(name = "安全配置")
public class SecurityController {

    @Resource
    private SecureRequestProperties secureRequestProperties;

    @GetMapping("/publicKey")
    @Operation(summary = "获取请求字段加密公钥")
    public Map<String, String> publicKey() {
        return Collections.singletonMap("publicKey", secureRequestProperties.getRsaPublicKey());
    }
}
