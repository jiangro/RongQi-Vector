package com.rongqi.vector.server.dto;

import com.rongqi.vector.core.JsonToString;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * HTTP 接口统一响应体。
 *
 * <p>所有 Controller 都返回这个结构，方便调用方统一处理成功和失败结果。</p>
 */
@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "success", data);
    }

    public static <T> ApiResponse<T> failed(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    @Override
    public String toString() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("success", success);
        summary.put("code", code);
        summary.put("message", message);
        summary.put("dataType", data == null ? null : data.getClass().getName());
        return JsonToString.toJson(summary);
    }
}
