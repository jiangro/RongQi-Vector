package com.rongqi.vector.core.metadata;

import com.rongqi.vector.annotation.IndexType;
import com.rongqi.vector.annotation.VectorCollection;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.annotation.VectorEmbeddingText;
import com.rongqi.vector.annotation.VectorField;
import com.rongqi.vector.annotation.VectorId;
import com.rongqi.vector.annotation.VectorIndex;
import com.rongqi.vector.annotation.VectorIndexes;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * DomainMetadataParser 单元测试。
 */
public class DomainMetadataParserTest {

    @Test
    public void parseShouldReadCollectionFieldsEmbeddingAndIndexes() {
        DomainMetadataParser parser = new DomainMetadataParser();

        VectorCollectionMetadata metadata = parser.parse(TestChunk.class);

        Assert.assertEquals("test_chunk_v1", metadata.getCollectionName());
        Assert.assertEquals("test_db", metadata.getDatabase());
        Assert.assertEquals("chunk_id", metadata.getIdField().getVectorName());
        Assert.assertEquals(4, metadata.getFields().size());
        Assert.assertEquals(1, metadata.getEmbeddingFields().size());
        Assert.assertEquals("content", metadata.getEmbeddingFields().get(0).getTextField());
        Assert.assertEquals("embedding", metadata.getEmbeddingFields().get(0).getVectorField());
        Assert.assertEquals(2, metadata.getIndexes().size());
    }

    @Test
    public void parseShouldInferSnakeCaseNameAndJavaType() {
        DomainMetadataParser parser = new DomainMetadataParser();

        VectorCollectionMetadata metadata = parser.parse(TestChunk.class);

        VectorFieldMetadata tenantField = metadata.findFieldByJavaName("tenantId").orElseThrow(AssertionError::new);
        Assert.assertEquals("tenant_id", tenantField.getVectorName());
        Assert.assertEquals(VectorDataType.INT64, tenantField.getType());
        Assert.assertTrue(tenantField.isFilterable());
    }

    @VectorCollection(name = "test_chunk_v1", database = "test_db")
    @VectorIndexes({
            @VectorIndex(field = "embedding", type = IndexType.HNSW, params = {"M=16"}),
            @VectorIndex(field = "tenantId", type = IndexType.INVERTED)
    })
    private static class TestChunk {
        @VectorId(name = "chunk_id")
        private String chunkId;

        @VectorField(filterable = true)
        private Long tenantId;

        @VectorEmbeddingText(vectorField = "embedding")
        @VectorField
        private String content;

        @VectorField(type = VectorDataType.FLOAT_VECTOR, dimension = 4, output = false)
        private List<Float> embedding;
    }
}

