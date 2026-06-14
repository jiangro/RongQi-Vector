package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入 Milvus 的普通字段或向量字段。
 *
 * <p>为了降低使用门槛，普通字段可以省略 type，框架会根据 Java 类型推断。
 * 向量字段需要声明 dimension，避免写入时发生维度不一致。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorField {

    /**
     * Milvus 字段名。为空时由 Java 字段名自动转 snake_case。
     */
    String name() default "";

    /**
     * Milvus 字段类型。AUTO 时根据 Java 字段类型推断。
     */
    VectorDataType type() default VectorDataType.AUTO;

    /**
     * VARCHAR 字段最大长度。
     */
    int maxLength() default -1;

    /**
     * 向量字段维度。只有向量字段需要填写。
     */
    int dimension() default -1;

    /**
     * 向量字段相似度计算方式。
     */
    MetricType metricType() default MetricType.COSINE;

    /**
     * 字段是否允许为空。
     */
    boolean nullable() default true;

    /**
     * 字段是否允许作为查询或删除条件。
     */
    boolean filterable() default false;

    /**
     * 查询结果中是否默认返回该字段。向量字段建议设置为 false。
     */
    boolean output() default true;
}

