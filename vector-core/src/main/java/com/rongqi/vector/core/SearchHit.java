package com.rongqi.vector.core;

/**
 * 单条向量检索结果。
 *
 * @param <T> 业务 domain 类型
 */
public class SearchHit<T> {
    private final double score;
    private final T entity;

    public SearchHit(double score, T entity) {
        this.score = score;
        this.entity = entity;
    }

    public double getScore() {
        return score;
    }

    public T getEntity() {
        return entity;
    }
}

