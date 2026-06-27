package com.rongqi.vector.core.rerank;

/**
 * 传给 Rerank 模型的候选文档。
 */
public class RerankDocument {
    private final int index;
    private final String id;
    private final String text;
    private final double vectorScore;

    /**
     * 创建候选文档。
     *
     * @param index 候选文档在原始结果列表中的位置
     * @param id 候选文档业务 id，允许为空
     * @param text 候选文档正文
     * @param vectorScore Milvus 第一阶段返回的向量相似度分数
     */
    public RerankDocument(int index, String id, String text, double vectorScore) {
        this.index = index;
        this.id = id;
        this.text = text;
        this.vectorScore = vectorScore;
    }

    public int getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public double getVectorScore() {
        return vectorScore;
    }
}
