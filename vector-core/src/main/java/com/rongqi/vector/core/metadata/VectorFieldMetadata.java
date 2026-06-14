package com.rongqi.vector.core.metadata;

import com.rongqi.vector.annotation.MetricType;
import com.rongqi.vector.annotation.VectorDataType;
import java.lang.reflect.Field;

/**
 * 单个 domain 字段解析后的元数据。
 */
public class VectorFieldMetadata {
    private final Field javaField;
    private final String javaName;
    private final String vectorName;
    private final VectorDataType type;
    private final int maxLength;
    private final int dimension;
    private final MetricType metricType;
    private final boolean id;
    private final boolean autoId;
    private final boolean filterable;
    private final boolean output;

    public VectorFieldMetadata(Field javaField, String vectorName, VectorDataType type, int maxLength,
                               int dimension, MetricType metricType, boolean id, boolean autoId,
                               boolean filterable, boolean output) {
        this.javaField = javaField;
        this.javaField.setAccessible(true);
        this.javaName = javaField.getName();
        this.vectorName = vectorName;
        this.type = type;
        this.maxLength = maxLength;
        this.dimension = dimension;
        this.metricType = metricType;
        this.id = id;
        this.autoId = autoId;
        this.filterable = filterable;
        this.output = output;
    }

    public Field getJavaField() {
        return javaField;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getVectorName() {
        return vectorName;
    }

    public VectorDataType getType() {
        return type;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getDimension() {
        return dimension;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public boolean isId() {
        return id;
    }

    public boolean isAutoId() {
        return autoId;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public boolean isOutput() {
        return output;
    }
}

