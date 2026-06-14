package com.rongqi.vector.server.dto;

import lombok.Getter;

/**
 * HTTP 写入或删除响应。
 */
@Getter
public class VectorWriteResponse {
    private final int count;

    public VectorWriteResponse(int count) {
        this.count = count;
    }
}
