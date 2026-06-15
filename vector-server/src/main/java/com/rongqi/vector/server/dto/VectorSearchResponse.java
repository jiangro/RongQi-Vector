package com.rongqi.vector.server.dto;

import java.util.List;
import lombok.Getter;
import lombok.ToString;

/**
 * HTTP 检索响应。
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class VectorSearchResponse {
    private final List<VectorSearchHitResponse> hits;

    public VectorSearchResponse(List<VectorSearchHitResponse> hits) {
        this.hits = hits;
    }

    @ToString.Include(name = "hitsSize")
    private int hitsSize() {
        return hits == null ? 0 : hits.size();
    }
}
