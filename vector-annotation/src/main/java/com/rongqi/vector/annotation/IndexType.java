package com.rongqi.vector.annotation;

/**
 * Milvus 索引类型。
 *
 * <p>索引用来提升检索速度。向量字段通常使用向量索引，普通标量字段通常使用倒排索引。</p>
 */
public enum IndexType {
    /**
     * HNSW 向量索引，适合高召回、低延迟的近似向量搜索场景。
     */
    HNSW,

    /**
     * IVF_FLAT 向量索引，先把向量分桶再搜索，适合数据量较大且希望控制搜索范围的场景。
     */
    IVF_FLAT,

    /**
     * IVF_SQ8 向量索引，在 IVF 基础上做量化压缩，可以节省内存，但精度可能略有损失。
     */
    IVF_SQ8,

    /**
     * Milvus 自动选择的向量索引类型，适合不想手动选择索引算法的场景。
     */
    AUTOINDEX,

    /**
     * 普通字段倒排索引，适合字符串、数字等标量字段的过滤查询。
     */
    INVERTED
}
