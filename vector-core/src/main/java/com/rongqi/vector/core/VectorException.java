package com.rongqi.vector.core;

/**
 * RongQi Vector 的统一运行时异常。
 *
 * <p>框架内部遇到配置、注解、过滤条件、Embedding 或 Milvus 调用错误时，
 * 都应抛出该异常，并携带明确错误码，方便 HTTP 服务转换为统一响应。</p>
 */
public class VectorException extends RuntimeException {
    private final VectorErrorCode code;

    public VectorException(VectorErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public VectorException(VectorErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public VectorErrorCode getCode() {
        return code;
    }
}

