package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个 Java domain 对应一个 Milvus Collection。
 *
 * <p>业务方只需要把这个注解加在自己的 domain 类上，RongQi Vector 就可以在启动时
 * 读取 collection 名称、database 和自动创建策略，避免在配置文件里重复维护 schema。</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorCollection {

    /**
     * Milvus Collection 名称，例如 knowledge_chunk_v1。
     */
    String name();

    /**
     * Milvus database 名称。为空时使用全局默认 database。
     */
    String database() default "";

    /**
     * Collection 描述，用于管理和排查问题。
     */
    String description() default "";

    /**
     * Collection 不存在时是否自动创建。
     */
    boolean autoCreate() default true;

    /**
     * 索引不存在时是否自动创建。
     */
    boolean autoCreateIndex() default true;

    /**
     * 启动时是否校验 Milvus 中已有 schema 和注解是否一致。
     */
    boolean validateSchema() default true;

    /**
     * 是否启用 Milvus 动态字段。
     */
    boolean dynamicFieldEnabled() default false;
}

