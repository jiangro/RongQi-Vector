package com.rongqi.vector.milvus;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

/**
 * 把 domain 条件对象转换成 Milvus filter 表达式。
 */
public class FilterExpressionBuilder {
    private final DomainValueAccessor accessor;

    public FilterExpressionBuilder(DomainValueAccessor accessor) {
        this.accessor = accessor;
    }

    /**
     * 根据 filter 对象中的非空字段生成 and 条件。
     */
    public String build(VectorCollectionMetadata metadata, Object filter) {
        if (filter == null) {
            return "";
        }
        List<String> conditions = new ArrayList<>();
        for (VectorFieldMetadata field : metadata.getFields()) {
            Object value = accessor.get(filter, field);
            if (value == null) {
                continue;
            }
            if (!field.isFilterable() && !field.isId()) {
                throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                        "字段不允许作为过滤条件: " + metadata.getDomainType().getName() + "." + field.getJavaName());
            }
            conditions.add(toCondition(field, value));
        }
        return String.join(" and ", conditions);
    }

    /**
     * 根据主键值生成删除条件。
     */
    public String buildIdFilter(VectorCollectionMetadata metadata, Object id) {
        if (id == null) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "按主键删除时 id 不能为空: " + metadata.getDomainType().getName());
        }
        return toCondition(metadata.getIdField(), id);
    }

    private String toCondition(VectorFieldMetadata field, Object value) {
        if (value instanceof Collection) {
            StringJoiner joiner = new StringJoiner(",", field.getVectorName() + " in [", "]");
            for (Object item : (Collection<?>) value) {
                joiner.add(formatValue(item));
            }
            return joiner.toString();
        }
        return field.getVectorName() + " == " + formatValue(value);
    }

    private String formatValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}

