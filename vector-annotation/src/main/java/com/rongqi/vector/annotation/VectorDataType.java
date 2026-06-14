package com.rongqi.vector.annotation;

/**
 * RongQi Vector 支持的字段类型。
 *
 * <p>AUTO 表示由框架根据 Java 字段类型自动推断，适合普通标量字段。
 * 向量字段建议显式声明为 FLOAT_VECTOR 并填写维度。</p>
 */
public enum VectorDataType {
    AUTO,
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,
    FLOAT,
    DOUBLE,
    VARCHAR,
    JSON,
    ARRAY,
    FLOAT_VECTOR,
    BINARY_VECTOR,
    SPARSE_FLOAT_VECTOR
}

