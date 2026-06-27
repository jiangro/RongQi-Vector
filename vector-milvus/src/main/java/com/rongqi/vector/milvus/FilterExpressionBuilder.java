package com.rongqi.vector.milvus;

import com.rongqi.vector.core.FilterCondition;
import com.rongqi.vector.core.FilterOperator;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将 domain 条件对象和显式搜索过滤条件转换成 Milvus filter 表达式。
 */
public class FilterExpressionBuilder {
    private final DomainValueAccessor accessor;
    private final MilvusFilterExpressionFormatter formatter;

    /**
     * 创建过滤表达式构建器。
     *
     * @param accessor domain 字段访问器
     */
    public FilterExpressionBuilder(DomainValueAccessor accessor) {
        this.accessor = accessor;
        this.formatter = new MilvusFilterExpressionFormatter();
    }

    /**
     * 根据对象条件生成 Milvus filter 表达式。
     *
     * @param metadata domain 元数据
     * @param filter 条件对象
     * @return Milvus filter 表达式
     */
    public String build(VectorCollectionMetadata metadata, Object filter) {
        return build(metadata, filter, Collections.emptyList());
    }

    /**
     * 根据对象条件和显式过滤条件生成 Milvus filter 表达式。
     *
     * @param metadata domain 元数据
     * @param filter 条件对象，非空字段按等值条件处理
     * @param filterConditions 显式过滤条件，支持范围、列表和模糊匹配
     * @return Milvus filter 表达式
     */
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

    /**
     * 根据主键值生成删除条件。
     *
     * @param metadata domain 元数据
     * @param id 主键值
     * @return 主键等值过滤表达式
     */
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
        return formatter.toCondition(fieldName, operator, value);
    }
}
