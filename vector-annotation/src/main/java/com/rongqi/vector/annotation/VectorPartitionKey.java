package com.rongqi.vector.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记分区键字段。
 *
 * <p>第一版可以先解析该注解并保留元数据，具体是否创建 Milvus partition key
 * 取决于 Milvus 服务端版本和 SDK 能力。</p>
 *
 * <p>分区键通常用于按租户、业务线等高频条件拆分数据，让查询可以更快定位到相关数据范围。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VectorPartitionKey {
}
