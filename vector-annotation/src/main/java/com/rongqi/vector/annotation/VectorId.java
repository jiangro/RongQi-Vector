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
     */
    String name() default "";

    /**
     * 主键字段类型。AUTO 时根据 Java 类型推断。
     */
    VectorDataType type() default VectorDataType.AUTO;

    /**
     * VARCHAR 主键的最大长度。
     */
    int maxLength() default 128;

    /**
     * 是否由 Milvus 自动生成主键。
     */
    boolean autoId() default false;
}

