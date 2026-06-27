package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 承载多个 {@link VectorIndex} 注解。
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorIndexes {
    /**
     * 多个 {@link VectorIndex} 注解的容器值。
     *
     * <p>业务代码一般不需要直接使用该属性；重复书写 {@link VectorIndex} 时 Java 会自动使用该容器。</p>
     */
    VectorIndex[] value();
}
