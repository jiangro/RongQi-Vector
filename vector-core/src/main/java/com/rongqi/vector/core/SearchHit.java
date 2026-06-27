package com.rongqi.vector.core;

/**
 * 单条向量检索结果。
 *
 * @param <T> 业务 domain 类型
 */
public class SearchHit<T> {
    private final double score;
    private final Double rankScore;
    private final T entity;

    public SearchHit(double score, T entity) {
        this(score, null, entity);
    }

    public SearchHit(double score, Double rankScore, T entity) {
        this.score = score;
        this.rankScore = rankScore;
        this.entity = entity;
    }

    public double getScore() {
        return score;
    }

    /**
     * 返回二次排序后的分数。
     *
     * <p>为空表示没有启用 rank/rerank，原始 {@link #getScore()} 仍然是 Milvus 返回的向量相似度分数。</p>
     */
    public Double getRankScore() {
        return rankScore;
    }

    public T getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return JsonToString.toJson(this);
    }
}
