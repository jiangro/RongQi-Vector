package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Milvus Collection 的主键字段。
 *
 * <p>每个 domain 必须且只能有一个主键字段。主键通常使用 String 对应 VARCHAR，
 * 或 Long 对应 INT64。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorId {

    /**
     * Milvus 字段名。为空时由 Java 字段名自动转 snake_case。
     *
     * <p>例如 Java 字段 chunkId 默认会转换为 Milvus 主键字段 chunk_id。</p>
     */
    String name() default "";

    /**
     * 主键字段类型。AUTO 时根据 Java 类型推断。
     *
     * <p>常用类型是 VARCHAR 或 INT64；String 通常映射为 VARCHAR，Long 通常映射为 INT64。</p>
     */
    VectorDataType type() default VectorDataType.AUTO;

    /**
     * VARCHAR 主键的最大长度。
     *
     * <p>只有字符串主键需要关注该值，建议预留足够长度存放业务 id。</p>
     */
    int maxLength() default 128;

    /**
     * 是否由 Milvus 自动生成主键。
     *
     * <p>如果设置为 true，写入时通常不需要手动传主键；如果业务需要自己控制 id，应保持 false。</p>
     */
    boolean autoId() default false;
}
