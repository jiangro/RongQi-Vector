package com.rongqi.vector.server.dto;

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
}

