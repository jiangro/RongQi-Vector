package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要生成 embedding 的文本字段。
 *
 * <p>保存数据时，如果 vectorField 指向的向量字段为空，框架会读取当前文本字段，
 * 调用指定的 EmbeddingProvider 生成向量。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorEmbeddingText {

    /**
     * 生成的向量要写入哪个 Java 字段。
     */
    String vectorField();

    /**
     * 指定 embedding provider 名称。为空时使用默认 provider。
     */
    String provider() default "";

    /**
     * 指定 embedding 模型名称。为空时使用 provider 默认模型。
     */
    String model() default "";
}

