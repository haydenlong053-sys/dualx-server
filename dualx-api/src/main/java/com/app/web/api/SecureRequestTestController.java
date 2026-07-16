package com.app.web.api;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.app.common.annotation.SecureRequest;
import com.app.common.config.SecureRequestProperties;
import com.app.common.enums.BaseResultCodeEnum;
import com.app.common.exception.DcException;
import com.app.common.util.RSACryptoUtil;
import com.app.web.api.req.SecureRequestTestReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/secure")
@Profile({"dev", "test", "uat", "test02", "test03"})
@Tag(name = "安全请求测试")
public class SecureRequestTestController {

    @Resource
    private SecureRequestProperties secureRequestProperties;

    @PostMapping("/encrypt")
    @Operation(summary = "RSA加密测试")
    public Map<String, Object> encrypt(@RequestBody SecureRequestTestReq req) {
        try {
            PublicKey publicKey = RSACryptoUtil.stringToPublicKey(secureRequestProperties.getRsaPublicKey());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("account", encryptIfNotBlank(req.getAccount(), publicKey));
            result.put("password", encryptIfNotBlank(req.getPassword(), publicKey));
            result.put("remark", req.getRemark());
            result.put("useTip", "把返回的account/password密文原样传给 /api/test/secure/echo，并按文档生成X-Timestamp、X-Nonce、X-Sign");
            return result;
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }

    public Map<String, Object> encryptBlock(SecureRequestTestReq req, BlockException e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 429);
        result.put("msg", "请求过于频繁，请稍后再试");
        result.put("resource", e.getRule() == null ? "testSecureEncrypt" : e.getRule().getResource());
        return result;
    }

    @PostMapping("/echo")
    @SecureRequest
    @Operation(summary = "安全请求签名和RSA解密测试")
    public Map<String, Object> echo(@RequestBody SecureRequestTestReq req) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signPass", true);
        result.put("account", req.getAccount());
        result.put("password", req.getPassword());
        result.put("remark", req.getRemark());
        return result;
    }

    @PostMapping("/sha256")
    @Operation(summary = "请求body的SHA256测试")
    public Map<String, Object> sha256(@RequestBody String body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("body", body);
        result.put("bodyHash", sha256Hex(body));
        result.put("useTip", "把bodyHash放到签名原文最后一行，再用sign-secret生成X-Sign");
        return result;
    }

    private String encryptIfNotBlank(String value, PublicKey publicKey) throws Exception {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return RSACryptoUtil.encrypt(value, publicKey);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new DcException(BaseResultCodeEnum.ERROR_PARAMETERS);
        }
    }
}
