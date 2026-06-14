package com.rongqi.vector.server.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务健康检查接口。
 */
@RestController
@RequestMapping("/api/vector")
public class VectorHealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("name", "RongQi Vector");
        response.put("status", "UP");
        return response;
    }
}

