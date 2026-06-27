package com.rongqi.vector.server.controller;

import com.rongqi.vector.core.VectorDiagnosis;
import com.rongqi.vector.core.VectorTemplate;
import com.rongqi.vector.server.dto.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 诊断接口，帮助用户检查 domain 注解和基础配置。
 */
@RestController
@RequestMapping("/api/vector")
public class VectorDiagnoseController {
    private final VectorTemplate vectorTemplate;

    public VectorDiagnoseController(VectorTemplate vectorTemplate) {
        this.vectorTemplate = vectorTemplate;
    }

    @GetMapping("/diagnose")
    public ApiResponse<Map<String, Object>> diagnose(@RequestParam("domain") String domainClassName)
            throws ClassNotFoundException {
        Class<?> domainType = Class.forName(domainClassName);
        VectorDiagnosis diagnosis = vectorTemplate.diagnose(domainType);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("healthy", diagnosis.isHealthy());
        data.put("messages", diagnosis.getMessages());
        return ApiResponse.success(data);
    }
}
