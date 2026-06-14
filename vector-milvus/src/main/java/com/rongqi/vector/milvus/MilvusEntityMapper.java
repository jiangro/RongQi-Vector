package com.rongqi.vector.milvus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * domain 对象和 Milvus 行数据之间的转换器。
 */
public class MilvusEntityMapper {
    private final DomainValueAccessor accessor;

    public MilvusEntityMapper(DomainValueAccessor accessor) {
        this.accessor = accessor;
    }

    /**
     * 把 domain 对象转换成 Milvus insert 需要的 JsonObject。
     */
    public JsonObject toRow(VectorCollectionMetadata metadata, Object entity) {
        JsonObject row = new JsonObject();
        for (VectorFieldMetadata field : metadata.getFields()) {
            Object value = accessor.get(entity, field);
            if (value == null && field.isAutoId()) {
                continue;
            }
            addValue(row, field, value);
        }
        return row;
    }

    /**
     * 把 Milvus 返回的 entity map 转换回业务 domain。
     */
    public <T> T toEntity(VectorCollectionMetadata metadata, Map<String, Object> values, Object id) {
        T entity = newInstance(metadata);
        for (VectorFieldMetadata field : metadata.getFields()) {
            Object value = values.get(field.getVectorName());
            if (value == null && field.isId()) {
                value = id;
            }
            if (value != null) {
                accessor.set(entity, field, value);
            }
        }
        return entity;
    }

    @SuppressWarnings("unchecked")
    private <T> T newInstance(VectorCollectionMetadata metadata) {
        try {
            Constructor<?> constructor = metadata.getDomainType().getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "domain 必须提供无参构造方法，推荐使用 Lombok @NoArgsConstructor: "
                            + metadata.getDomainType().getName(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void addValue(JsonObject row, VectorFieldMetadata field, Object value) {
        if (value == null) {
            value = defaultValue(field);
        }
        switch (field.getType()) {
            case BOOL:
                row.addProperty(field.getVectorName(), (Boolean) value);
                break;
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            case FLOAT:
            case DOUBLE:
                row.addProperty(field.getVectorName(), (Number) value);
                break;
            case VARCHAR:
                row.addProperty(field.getVectorName(), truncate(String.valueOf(value), field.getMaxLength()));
                break;
            case FLOAT_VECTOR:
                row.add(field.getVectorName(), toFloatArray((List<Float>) value));
                break;
            default:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "暂不支持写入字段类型 " + field.getType() + ": " + field.getJavaName());
        }
    }

    private JsonArray toFloatArray(List<Float> vector) {
        JsonArray array = new JsonArray();
        for (Float value : vector) {
            array.add(value);
        }
        return array;
    }

    private Object defaultValue(VectorFieldMetadata field) {
        if (field.isId()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "主键字段不能为空: " + field.getJavaName());
        }
        switch (field.getType()) {
            case BOOL:
                return false;
            case INT8:
            case INT16:
            case INT32:
            case INT64:
                return 0L;
            case FLOAT:
            case DOUBLE:
                return 0D;
            case VARCHAR:
                return "";
            case FLOAT_VECTOR:
            case BINARY_VECTOR:
            case SPARSE_FLOAT_VECTOR:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "向量字段不能为空，请传入 " + field.getJavaName() + " 或配置 @VectorEmbeddingText 自动生成");
            default:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "字段缺失且暂不支持默认值: " + field.getJavaName() + ", type=" + field.getType());
        }
    }

    private String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
