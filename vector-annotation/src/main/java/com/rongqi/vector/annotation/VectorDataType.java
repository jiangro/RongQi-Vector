package com.rongqi.vector.annotation;

/**
 * RongQi Vector 支持的字段类型。
 *
 * <p>AUTO 表示由框架根据 Java 字段类型自动推断，适合普通标量字段。
 * 向量字段建议显式声明为 FLOAT_VECTOR 并填写维度。</p>
 */
public enum VectorDataType {
    /**
     * 自动推断字段类型，框架会根据 Java 字段类型映射到 Milvus 类型。
     */
    AUTO,

    /**
     * 布尔类型，对应 true 或 false。
     */
    BOOL,

    /**
     * 8 位整数类型，适合很小范围的整数值。
     */
    INT8,

    /**
     * 16 位整数类型，适合小范围的整数值。
     */
    INT16,

    /**
     * 32 位整数类型，适合普通整数值。
     */
    INT32,

    /**
     * 64 位整数类型，适合 Long、业务编号、租户编号等较大整数值。
     */
    INT64,

    /**
     * 单精度浮点数类型。
     */
    FLOAT,

    /**
     * 双精度浮点数类型。
     */
    DOUBLE,

    /**
     * 字符串类型，需要配合 maxLength 指定最大长度。
     */
    VARCHAR,

    /**
     * JSON 类型，适合存储结构化扩展信息。
     */
    JSON,

    /**
     * 数组类型，适合存储同类型的多个值。
     */
    ARRAY,

    /**
     * 浮点向量类型，文本 embedding 最常用的向量字段类型，需要配置 dimension。
     */
    FLOAT_VECTOR,

    /**
     * 二进制向量类型，适合二值化向量检索场景。
     */
    BINARY_VECTOR,

    /**
     * 稀疏浮点向量类型，适合稀疏向量或混合检索场景。
     */
    SPARSE_FLOAT_VECTOR
}
