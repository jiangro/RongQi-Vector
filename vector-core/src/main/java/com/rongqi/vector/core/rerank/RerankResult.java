package com.rongqi.vector.core.rerank;

/**
 * Rerank 模型返回的单条打分结果。
 */
public class RerankResult {
    private final int index;
    private final double score;

    /**
     * 创建 rerank 结果。
     *
     * @param index 对应 {@link RerankDocument#getIndex()}
     * @param score rerank 模型返回的新分数
     */
    public RerankResult(int index, double score) {
        this.index = index;
        this.score = score;
    }

    public int getIndex() {
        return index;
    }

    public double getScore() {
        return score;
    }
}
