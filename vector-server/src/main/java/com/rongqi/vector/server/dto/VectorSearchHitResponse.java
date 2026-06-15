package com.rongqi.vector.server.dto;

import com.rongqi.vector.core.JsonToString;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * HTTP 检索命中的单条结果。
 */
@Getter
public class VectorSearchHitResponse {
    private final double score;
    private final Object entity;

    public VectorSearchHitResponse(double score, Object entity) {
        this.score = score;
        this.entity = entity;
    }

    @Override
    public String toString() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("score", score);
        summary.put("entityType", entity == null ? null : entity.getClass().getName());
        return JsonToString.toJson(summary);
    }
}
