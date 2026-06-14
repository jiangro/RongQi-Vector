package com.rongqi.vector.core.schema;

import com.rongqi.vector.annotation.MetricType;
import com.rongqi.vector.annotation.VectorDataType;

/**
 * HTTP 或配置方式定义的字段信息。
 *
 * <p>它和 @VectorField 的作用一致，只是不依赖 Java domain 类，适合纯 HTTP 用户创建 Collection。</p>
 */
public class VectorFieldDefinition {
    private String name;
    private VectorDataType type = VectorDataType.AUTO;
    private boolean primaryKey;
    private boolean autoId;
    private int maxLength = -1;
    private int dimension = -1;
    private MetricType metricType = MetricType.COSINE;
    private boolean nullable = true;
    private boolean filterable;
    private boolean output = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VectorDataType getType() {
        return type;
    }

    public void setType(VectorDataType type) {
        this.type = type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isAutoId() {
        return autoId;
    }

    public void setAutoId(boolean autoId) {
        this.autoId = autoId;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isOutput() {
        return output;
    }

    public void setOutput(boolean output) {
        this.output = output;
    }
}

