package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 Milvus 索引。
 *
 * <p>可以加在 domain 类上集中声明多个索引，也可以加在字段上声明单字段索引。</p>
 */
@Documented
@Repeatable(VectorIndexes.class)
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorIndex {

    /**
     * Java 字段名。注解加在字段上时可以省略。
     */
    String field() default "";

    /**
     * 索引名称。为空时由框架生成。
     */
    String name() default "";

    /**
     * 索引类型。
     */
    IndexType type() default IndexType.INVERTED;

    /**
     * 向量索引的相似度计算方式。
     */
    MetricType metricType() default MetricType.COSINE;

    /**
     * 索引参数，格式为 key=value，例如 {"M=16", "efConstruction=200"}。
     */
    String[] params() default {};
}

