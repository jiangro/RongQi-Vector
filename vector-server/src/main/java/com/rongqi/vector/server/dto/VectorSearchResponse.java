package com.rongqi.vector.server.dto;

import java.util.List;
import lombok.Getter;

/**
 * HTTP 检索响应。
 */
@Getter
public class VectorSearchResponse {
    private final List<VectorSearchHitResponse> hits;

    public VectorSearchResponse(List<VectorSearchHitResponse> hits) {
        this.hits = hits;
    }
}

