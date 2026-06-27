package com.rongqi.vector.annotation;

/**
 * 向量相似度计算方式。
 *
 * <p>搜索时 Milvus 会按照这里的算法计算 query 向量和库中向量的相似程度。</p>
 */
public enum MetricType {
    /**
     * 余弦相似度，关注向量方向，常用于文本语义向量检索。
     */
    COSINE,

    /**
     * 欧氏距离，关注两个向量在空间中的直线距离，距离越小越相似。
     */
    L2,

    /**
     * 内积相似度，常用于已经归一化或模型推荐使用内积的向量。
     */
    IP
}
