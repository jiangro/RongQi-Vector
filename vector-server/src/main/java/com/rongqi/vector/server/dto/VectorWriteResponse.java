package com.rongqi.vector.server.dto;

import com.rongqi.vector.core.JsonToString;
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

    @Override
    public String toString() {
        return JsonToString.toJson(this);
    }
}
