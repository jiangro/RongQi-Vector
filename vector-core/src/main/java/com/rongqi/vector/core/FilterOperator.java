package com.rongqi.vector.core;

/**
 * 向量搜索过滤操作符。
 */
public enum FilterOperator {
    /** 等于。 */
    EQ,

    /** 不等于。 */
    NE,

    /** 大于。 */
    GT,

    /** 大于等于。 */
    GTE,

    /** 小于。 */
    LT,

    /** 小于等于。 */
    LTE,

    /** 字段值在给定集合中。 */
    IN,

    /** 字段值不在给定集合中。 */
    NOT_IN,

    /** 字符串模糊匹配，由底层 Milvus filter 表达式执行。 */
    LIKE
}
