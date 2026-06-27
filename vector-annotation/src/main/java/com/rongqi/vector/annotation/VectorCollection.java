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
     *
     * <p>这个名称会作为 Milvus 中真实的 Collection 名称，建议使用稳定的英文小写和下划线。</p>
     */
    String name();

    /**
     * Milvus database 名称。为空时使用全局默认 database。
     *
     * <p>如果你的 Milvus 只使用 default database，可以保持默认空字符串。</p>
     */
    String database() default "";

    /**
     * Collection 描述，用于管理和排查问题。
     *
     * <p>描述不会影响检索逻辑，只用于让维护人员知道这个 Collection 存什么数据。</p>
     */
    String description() default "";

    /**
     * Collection 不存在时是否自动创建。
     *
     * <p>开发和测试环境建议保持 true；生产环境如果希望由 DBA 预先建表，可以设置为 false。</p>
     */
    boolean autoCreate() default true;

    /**
     * 索引不存在时是否自动创建。
     *
     * <p>开启后框架会按照 {@link VectorIndex} 配置创建索引，避免忘记建索引导致搜索很慢。</p>
     */
    boolean autoCreateIndex() default true;

    /**
     * 启动时是否校验 Milvus 中已有 schema 和注解是否一致。
     *
     * <p>建议保持 true。这样字段类型、向量维度等配置不一致时会尽早报错。</p>
     */
    boolean validateSchema() default true;

    /**
     * 是否启用 Milvus 动态字段。
     *
     * <p>关闭时只能写入 schema 中明确定义的字段，适合大多数业务系统，能减少脏数据。</p>
     */
    boolean dynamicFieldEnabled() default false;
}
