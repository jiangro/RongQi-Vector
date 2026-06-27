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
     *
     * <p>注解加在类上时必须指定，用来告诉框架要给哪个字段建索引。</p>
     */
    String field() default "";

    /**
     * 索引名称。为空时由框架生成。
     *
     * <p>通常可以不填；如果公司有统一索引命名规范，可以在这里显式指定。</p>
     */
    String name() default "";

    /**
     * 索引类型。
     *
     * <p>向量字段通常使用 HNSW、IVF_FLAT、IVF_SQ8 或 AUTOINDEX；普通过滤字段通常使用 INVERTED。</p>
     */
    IndexType type() default IndexType.INVERTED;

    /**
     * 向量索引的相似度计算方式。
     *
     * <p>该值应和向量字段的 metricType 保持一致，避免索引和搜索使用不同的距离算法。</p>
     */
    MetricType metricType() default MetricType.COSINE;

    /**
     * 索引参数，格式为 key=value，例如 {"M=16", "efConstruction=200"}。
     *
     * <p>不同索引类型支持的参数不同；不熟悉时可以先留空，使用 Milvus 或框架默认值。</p>
     */
    String[] params() default {};
}
