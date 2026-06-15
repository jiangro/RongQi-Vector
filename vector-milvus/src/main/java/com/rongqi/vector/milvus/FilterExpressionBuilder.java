package com.rongqi.vector.milvus;

import com.rongqi.vector.core.FilterCondition;
import com.rongqi.vector.core.FilterOperator;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * 将 domain 条件对象和显式搜索过滤条件转换成 Milvus filter 表达式。
 */
public class FilterExpressionBuilder {
    private final DomainValueAccessor accessor;

    public FilterExpressionBuilder(DomainValueAccessor accessor) {
        this.accessor = accessor;
    }

    public String build(VectorCollectionMetadata metadata, Object filter) {
        return build(metadata, filter, Collections.emptyList());
    }

    public String build(VectorCollectionMetadata metadata, Object filter, List<FilterCondition> filterConditions) {
        List<String> conditions = new ArrayList<>();
        if (filter != null) {
            // 兼容旧用法：filter 对象中的非空字段仍然按等值条件处理。
            for (VectorFieldMetadata field : metadata.getFields()) {
                Object value = accessor.get(filter, field);
                if (value == null) {
                    continue;
                }
                validateFilterable(metadata, field);
                conditions.add(toCondition(field, FilterOperator.EQ, value));
            }
        }
        if (filterConditions != null) {
            // 新用法：SearchOptions 中的显式条件支持范围、列表和模糊匹配。
            for (FilterCondition condition : filterConditions) {
                if (condition == null || condition.getField() == null || condition.getField().trim().isEmpty()) {
                    continue;
                }
                VectorFieldMetadata field = metadata.findFieldByName(condition.getField())
                        .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                                "过滤字段不存在: " + condition.getField()));
                validateFilterable(metadata, field);
                conditions.add(toCondition(field, condition.getOperator(), condition.getValue()));
            }
        }
        return String.join(" and ", conditions);
    }

    public String buildIdFilter(VectorCollectionMetadata metadata, Object id) {
        if (id == null) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "按主键删除时 id 不能为空: " + metadata.getDomainType().getName());
        }
        return toCondition(metadata.getIdField(), FilterOperator.EQ, id);
    }

    private void validateFilterable(VectorCollectionMetadata metadata, VectorFieldMetadata field) {
        if (!field.isFilterable() && !field.isId()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "字段不允许作为过滤条件: " + metadata.getDomainType().getName() + "." + field.getJavaName());
        }
    }

    private String toCondition(VectorFieldMetadata field, FilterOperator operator, Object value) {
        return toCondition(field.getVectorName(), operator, value);
    }

    private String toCondition(String fieldName, FilterOperator operator, Object value) {
        if (value == null) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "过滤条件的值不能为空: " + fieldName);
        }
        FilterOperator actualOperator = operator == null ? FilterOperator.EQ : operator;
        switch (actualOperator) {
            case EQ:
                if (value instanceof Collection) {
                    return toListCondition(fieldName, "in", value);
                }
                return fieldName + " == " + formatValue(value);
            case NE:
                return fieldName + " != " + formatValue(value);
            case GT:
                return fieldName + " > " + formatValue(value);
            case GTE:
                return fieldName + " >= " + formatValue(value);
            case LT:
                return fieldName + " < " + formatValue(value);
            case LTE:
                return fieldName + " <= " + formatValue(value);
            case IN:
                return toListCondition(fieldName, "in", value);
            case NOT_IN:
                return toListCondition(fieldName, "not in", value);
            case LIKE:
                if (!(value instanceof CharSequence)) {
                    throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                            "like 过滤条件的值必须是字符串: " + fieldName);
                }
                return fieldName + " like " + formatValue(value);
            default:
                throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                        "不支持的过滤操作符: " + actualOperator);
        }
    }

    private String toListCondition(String fieldName, String operator, Object value) {
        if (!(value instanceof Collection)) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    operator + " 过滤条件的值必须是集合: " + fieldName);
        }
        Collection<?> values = (Collection<?>) value;
        if (values.isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    operator + " 过滤条件的值不能为空: " + fieldName);
        }
        StringJoiner joiner = new StringJoiner(",", fieldName + " " + operator + " [", "]");
        for (Object item : values) {
            joiner.add(formatValue(item));
        }
        return joiner.toString();
    }

    private String formatValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
