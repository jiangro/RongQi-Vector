package com.rongqi.vector.core;

/**
 * 向量搜索过滤条件。
 *
 * <p>该对象用于表达大于、小于、列表包含、模糊匹配等复杂过滤条件。</p>
 */
public class FilterCondition {
    private String field;
    private FilterOperator operator = FilterOperator.EQ;
    private Object value;

    public FilterCondition() {
    }

    /**
     * 创建一个过滤条件。
     *
     * @param field 字段名，注解模式可传 Java 字段名或 Milvus 字段名，HTTP collection 模式传 schema 字段名
     * @param operator 过滤操作符，为空时按等于处理
     * @param value 过滤值，IN 和 NOT_IN 必须传集合
     */
    public FilterCondition(String field, FilterOperator operator, Object value) {
        this.field = field;
        this.operator = operator == null ? FilterOperator.EQ : operator;
        this.value = value;
    }

    /**
     * 创建一个过滤条件。
     *
     * @param field 字段名，注解模式可传 Java 字段名或 Milvus 字段名，HTTP collection 模式传 schema 字段名
     * @param operator 过滤操作符，为空时按等于处理
     * @param value 过滤值，IN 和 NOT_IN 必须传集合
     * @return 过滤条件对象
     */
    public static FilterCondition of(String field, FilterOperator operator, Object value) {
        return new FilterCondition(field, operator, value);
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public FilterOperator getOperator() {
        return operator;
    }

    public void setOperator(FilterOperator operator) {
        this.operator = operator == null ? FilterOperator.EQ : operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
