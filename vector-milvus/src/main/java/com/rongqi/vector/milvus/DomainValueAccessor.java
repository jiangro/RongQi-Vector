package com.rongqi.vector.milvus;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import java.lang.reflect.Field;

/**
 * 读取和写入 domain 字段值。
 *
 * <p>这里直接基于字段反射访问，所以业务方可以使用 Lombok 生成 getter/setter，
 * 也可以完全不写 getter/setter。</p>
 */
public class DomainValueAccessor {

    /**
     * 读取字段值。
     */
    public Object get(Object entity, VectorFieldMetadata fieldMetadata) {
        try {
            Field field = fieldMetadata.getJavaField();
            return field.get(entity);
        } catch (IllegalAccessException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "读取 domain 字段失败: " + fieldMetadata.getJavaName(), exception);
        }
    }

    /**
     * 写入字段值。
     */
    public void set(Object entity, VectorFieldMetadata fieldMetadata, Object value) {
        try {
            Field field = fieldMetadata.getJavaField();
            field.set(entity, value);
        } catch (IllegalAccessException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "写入 domain 字段失败: " + fieldMetadata.getJavaName(), exception);
        }
    }
}

