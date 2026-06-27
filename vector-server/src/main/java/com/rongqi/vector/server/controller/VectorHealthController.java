package com.rongqi.vector.server.controller;

import com.rongqi.vector.server.dto.ApiResponse;
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
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "RongQi Vector");
        data.put("status", "UP");
        return ApiResponse.success(data);
    }
}
