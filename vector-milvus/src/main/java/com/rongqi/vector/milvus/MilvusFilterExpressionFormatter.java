package com.rongqi.vector.milvus;

import com.rongqi.vector.core.FilterOperator;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import java.util.Collection;
import java.util.StringJoiner;

/**
 * Milvus 过滤表达式格式化工具。
 *
 * <p>该类集中维护 EQ、IN、LIKE 等表达式拼接规则，避免 domain 模式和 HTTP collection 模式各自维护一份。</p>
 */
class MilvusFilterExpressionFormatter {

    /**
     * 将字段名、操作符和值转换成 Milvus filter 条件。
     *
     * @param fieldName Milvus schema 字段名
     * @param operator 过滤操作符，为空时默认按 EQ 处理
     * @param value 过滤值
     * @return 单个 Milvus filter 条件
     */
    String toCondition(String fieldName, FilterOperator operator, Object value) {
        if (value == null) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "过滤条件的值不能为空: " + fieldName);
        }
        FilterOperator actualOperator = operator == null ? FilterOperator.EQ : operator;
        switch (actualOperator) {
            case EQ:
                // 兼容旧用法：EQ 遇到集合值时自动转换为 in 条件。
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
                return fieldName + " like " + formatValue(normalizeLikePattern(value));
            default:
                throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                        "不支持的过滤操作符: " + actualOperator);
        }
    }

    /**
     * 将 Java 值格式化为 Milvus filter 可识别的字面量。
     */
    String formatValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * 将集合值转换成 Milvus 的 in / not in 条件。
     */
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

    /**
     * 规范化 LIKE 条件。
     *
     * <p>业务用户通常只关心要搜索的关键词，因此没有显式传入 % 时，默认按包含匹配处理。</p>
     */
    private String normalizeLikePattern(Object value) {
        String pattern = String.valueOf(value);
        if (pattern.contains("%")) {
            return pattern;
        }
        return "%" + pattern + "%";
    }
}
