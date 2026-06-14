package com.rongqi.vector.milvus;

import com.rongqi.vector.annotation.VectorCollection;
import com.rongqi.vector.annotation.VectorField;
import com.rongqi.vector.annotation.VectorId;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.DomainMetadataParser;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import org.junit.Assert;
import org.junit.Test;

/**
 * FilterExpressionBuilder 单元测试。
 */
public class FilterExpressionBuilderTest {

    @Test
    public void buildShouldUseNonNullFilterableFields() {
        VectorCollectionMetadata metadata = new DomainMetadataParser().parse(TestFilter.class);
        FilterExpressionBuilder builder = new FilterExpressionBuilder(new DomainValueAccessor());
        TestFilter filter = new TestFilter();
        filter.tenantId = 1001L;
        filter.businessCode = "faq";

        String expression = builder.build(metadata, filter);

        Assert.assertEquals("tenant_id == 1001 and business_code == \"faq\"", expression);
    }

    @Test
    public void buildShouldRejectNonFilterableField() {
        VectorCollectionMetadata metadata = new DomainMetadataParser().parse(TestFilter.class);
        FilterExpressionBuilder builder = new FilterExpressionBuilder(new DomainValueAccessor());
        TestFilter filter = new TestFilter();
        filter.title = "not allowed";

        Assert.assertThrows(VectorException.class, () -> builder.build(metadata, filter));
    }

    @VectorCollection(name = "test_filter_v1")
    private static class TestFilter {
        @VectorId
        private String chunkId;

        @VectorField(filterable = true)
        private Long tenantId;

        @VectorField(name = "business_code", filterable = true)
        private String businessCode;

        @VectorField
        private String title;
    }
}
