package com.rongqi.vector.examples;

import com.rongqi.vector.annotation.IndexType;
import com.rongqi.vector.annotation.MetricType;
import com.rongqi.vector.annotation.VectorCollection;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.annotation.VectorEmbeddingText;
import com.rongqi.vector.annotation.VectorField;
import com.rongqi.vector.annotation.VectorId;
import com.rongqi.vector.annotation.VectorIndex;
import com.rongqi.vector.annotation.VectorIndexes;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 最小知识片段示例。
 *
 * <p>业务系统可以参考这个类定义自己的 domain。getter/setter 由 Lombok 自动生成，
 * 不需要手写样板代码。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@VectorCollection(
        name = "knowledge_chunk_v1",
        database = "knowledge_base",
        description = "知识片段向量集合"
)
@VectorIndexes({
        @VectorIndex(field = "embedding", name = "idx_embedding", type = IndexType.HNSW,
                metricType = MetricType.COSINE, params = {"M=16", "efConstruction=200"}),
        @VectorIndex(field = "tenantId", name = "idx_tenant_id", type = IndexType.INVERTED),
        @VectorIndex(field = "businessCode", name = "idx_business_code", type = IndexType.INVERTED)
})
public class KnowledgeChunk {

    /**
     * 业务主键，对应 Milvus 主键字段 chunk_id。
     */
    @VectorId(name = "chunk_id", type = VectorDataType.VARCHAR, maxLength = 128)
    private String chunkId;

    /**
     * 租户 ID，常用于多租户隔离和查询过滤。
     */
    @VectorField(name = "tenant_id", filterable = true)
    private Long tenantId;

    /**
     * 业务数据 ID，例如文档 ID、商品 ID、题目 ID。
     */
    @VectorField(name = "biz_id", maxLength = 128, filterable = true)
    private String bizId;

    /**
     * 业务分类编码。
     */
    @VectorField(name = "business_code", maxLength = 64, filterable = true)
    private String businessCode;

    /**
     * 标题默认作为返回字段。
     */
    @VectorField(maxLength = 512)
    private String title;

    /**
     * 需要生成 embedding 的正文内容。
     */
    @VectorEmbeddingText(vectorField = "embedding")
    @VectorField(maxLength = 8192)
    private String content;

    /**
     * 向量字段通常不需要返回给业务方。
     */
    @VectorField(type = VectorDataType.FLOAT_VECTOR, dimension = 1024,
            metricType = MetricType.COSINE, output = false)
    private List<Float> embedding;
}
