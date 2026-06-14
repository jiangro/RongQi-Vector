package com.rongqi.vector.core.metadata;

import com.rongqi.vector.annotation.IndexType;
import com.rongqi.vector.annotation.MetricType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个 Milvus 索引解析后的元数据。
 */
public class VectorIndexMetadata {
    private final String javaFieldName;
    private final String indexName;
    private final IndexType indexType;
    private final MetricType metricType;
    private final Map<String, Object> params;

    public VectorIndexMetadata(String javaFieldName, String indexName, IndexType indexType,
                               MetricType metricType, Map<String, Object> params) {
        this.javaFieldName = javaFieldName;
        this.indexName = indexName;
        this.indexType = indexType;
        this.metricType = metricType;
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    public String getJavaFieldName() {
        return javaFieldName;
    }

    public String getIndexName() {
        return indexName;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}

