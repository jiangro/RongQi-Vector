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
     *
     * <p>例如 Java 字段 tenantId 默认会转换为 Milvus 字段 tenant_id。</p>
     */
    String name() default "";

    /**
     * Milvus 字段类型。AUTO 时根据 Java 字段类型推断。
     *
     * <p>普通字符串、数字、布尔字段可以使用 AUTO；向量字段建议显式设置为 FLOAT_VECTOR。</p>
     */
    VectorDataType type() default VectorDataType.AUTO;

    /**
     * VARCHAR 字段最大长度。
     *
     * <p>只有字符串字段需要关注该值。小于等于 0 时框架会使用默认长度。</p>
     */
    int maxLength() default -1;

    /**
     * 向量字段维度。只有向量字段需要填写。
     *
     * <p>该值必须和 Embedding 模型输出维度一致，例如模型输出 1024 维，这里就填 1024。</p>
     */
    int dimension() default -1;

    /**
     * 向量字段相似度计算方式。
     *
     * <p>文本向量检索通常使用 COSINE；如果模型文档要求 IP 或 L2，应按模型要求配置。</p>
     */
    MetricType metricType() default MetricType.COSINE;

    /**
     * 字段是否允许为空。
     *
     * <p>该配置用于表达业务字段约束，写入前建议保证必填字段不为空。</p>
     */
    boolean nullable() default true;

    /**
     * 字段是否允许作为查询或删除条件。
     *
     * <p>需要用于 filter、filters 或 delete 条件的字段必须设置为 true，主键字段除外。</p>
     */
    boolean filterable() default false;

    /**
     * 查询结果中是否默认返回该字段。向量字段建议设置为 false。
     *
     * <p>关闭后默认搜索结果不会返回该字段，可以减少响应体大小，尤其适合 embedding 向量字段。</p>
     */
    boolean output() default true;
}
