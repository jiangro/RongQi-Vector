package com.rongqi.vector.server.dto;

import lombok.Getter;
import lombok.ToString;

/**
 * HTTP 检索命中的单条结果。
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class VectorSearchHitResponse {
    @ToString.Include
    private final double score;
    private final Object entity;

    public VectorSearchHitResponse(double score, Object entity) {
        this.score = score;
        this.entity = entity;
    }

    @ToString.Include(name = "entityType")
    private String entityType() {
        return entity == null ? null : entity.getClass().getName();
    }
}
