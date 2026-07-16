package com.app.web.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/sentinel")
@Profile({"dev", "test", "uat", "test02", "test03"})
@Tag(name = "Sentinel测试")
public class SentinelTestController {

    @GetMapping("/ping")
    @Operation(summary = "Sentinel资源统计测试")
    public Map<String, Object> ping() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", true);
        result.put("message", "sentinel pong");
        result.put("timeMillis", System.currentTimeMillis());
        return result;
    }
}
