package com.rongqi.vector.server.dto;

import lombok.Getter;
import lombok.ToString;

/**
 * HTTP 接口统一响应体。
 *
 * <p>所有 Controller 都返回这个结构，方便调用方统一处理成功和失败结果。</p>
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class ApiResponse<T> {
    @ToString.Include
    private final boolean success;
    @ToString.Include
    private final String code;
    @ToString.Include
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

    @ToString.Include(name = "dataType")
    private String dataType() {
        return data == null ? null : data.getClass().getName();
    }
}
