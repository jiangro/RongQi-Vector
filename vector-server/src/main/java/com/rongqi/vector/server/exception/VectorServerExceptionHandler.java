package com.rongqi.vector.server.exception;

import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.server.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * HTTP 服务统一异常处理。
 */
@RestControllerAdvice
public class VectorServerExceptionHandler {

    /**
     * RongQi Vector 业务异常转成统一响应。
     */
    @ExceptionHandler(VectorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleVectorException(VectorException exception) {
        return ApiResponse.failed(exception.getCode().name(), exception.getMessage());
    }

    /**
     * 未预期异常转成统一响应，避免直接暴露堆栈。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.failed("VECTOR_SERVER_ERROR", rootMessage(exception));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return current.getClass().getName();
        }
        return message;
    }
}
