package com.rongqi.vector.core.schema;

import com.rongqi.vector.annotation.IndexType;
import com.rongqi.vector.annotation.MetricType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 或配置方式定义的索引信息。
 */
public class VectorIndexDefinition {
    private String field;
    private String name;
    private IndexType type = IndexType.INVERTED;
    private MetricType metricType = MetricType.COSINE;
    private Map<String, Object> params = new LinkedHashMap<>();

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IndexType getType() {
        return type;
    }

    public void setType(IndexType type) {
        this.type = type;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : params;
    }
}

