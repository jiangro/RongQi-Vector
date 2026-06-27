package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记不写入 Milvus 的 Java 字段。
 *
 * <p>适合临时计算字段、页面展示字段、非持久化字段等。被标记的字段不会参与 schema 解析、
 * 写入、搜索结果映射和过滤条件构建。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorIgnore {
}
