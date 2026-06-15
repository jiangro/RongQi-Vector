package com.rongqi.vector.server.dto;

import com.rongqi.vector.core.JsonToString;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public String toString() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hitsSize", hits == null ? 0 : hits.size());
        return JsonToString.toJson(summary);
    }
}
