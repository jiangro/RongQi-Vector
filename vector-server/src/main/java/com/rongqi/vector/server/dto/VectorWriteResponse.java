package com.rongqi.vector.server.dto;

import lombok.Getter;
import lombok.ToString;

/**
 * HTTP 写入或删除响应。
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class VectorWriteResponse {
    @ToString.Include
    private final int count;

    public VectorWriteResponse(int count) {
        this.count = count;
    }
}
