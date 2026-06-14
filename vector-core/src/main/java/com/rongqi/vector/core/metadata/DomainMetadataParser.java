package com.rongqi.vector.core.metadata;

import com.rongqi.vector.annotation.VectorIndex;
import com.rongqi.vector.annotation.VectorCollection;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.annotation.VectorEmbeddingText;
import com.rongqi.vector.annotation.VectorField;
import com.rongqi.vector.annotation.VectorId;
import com.rongqi.vector.annotation.VectorIgnore;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析业务 domain 上的 RongQi Vector 注解。
 *
 * <p>该类是注解驱动设计的入口，负责把用户易读的 Java 注解转换成框架内部元数据。</p>
 */
public class DomainMetadataParser {

    /**
     * 解析一个 domain 类。
     *
     * @param domainType 用户定义的 domain 类型
     * @return collection 元数据
     */
    public VectorCollectionMetadata parse(Class<?> domainType) {
        VectorCollection collection = domainType.getAnnotation(VectorCollection.class);
        if (collection == null) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "domain 缺少 @VectorCollection: " + domainType.getName());
        }

        List<VectorFieldMetadata> fields = new ArrayList<>();
        List<EmbeddingFieldMetadata> embeddingFields = new ArrayList<>();
        List<VectorIndexMetadata> indexes = new ArrayList<>();
        int idCount = 0;

        for (VectorIndex index : domainType.getAnnotationsByType(VectorIndex.class)) {
            indexes.add(parseIndex(index, index.field()));
        }

        for (Field field : domainType.getDeclaredFields()) {
            if (field.isAnnotationPresent(VectorIgnore.class)) {
                continue;
            }
            VectorId id = field.getAnnotation(VectorId.class);
            VectorField vectorField = field.getAnnotation(VectorField.class);
            VectorEmbeddingText embeddingText = field.getAnnotation(VectorEmbeddingText.class);

            if (id != null) {
                idCount++;
                fields.add(parseIdField(field, id));
            } else if (vectorField != null) {
                fields.add(parseNormalField(field, vectorField));
            }

            if (embeddingText != null) {
                embeddingFields.add(new EmbeddingFieldMetadata(
                        field.getName(),
                        embeddingText.vectorField(),
                        embeddingText.provider(),
                        embeddingText.model()));
            }
            for (VectorIndex index : field.getAnnotationsByType(VectorIndex.class)) {
                indexes.add(parseIndex(index, field.getName()));
            }
        }

        if (idCount != 1) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "domain 必须且只能有一个 @VectorId: " + domainType.getName());
        }
        validateEmbeddingTargets(domainType, fields, embeddingFields);

        return new VectorCollectionMetadata(
                domainType,
                collection.name(),
                collection.database(),
                collection.description(),
                collection.autoCreate(),
                collection.autoCreateIndex(),
                collection.validateSchema(),
                fields,
                embeddingFields,
                indexes);
    }

    private VectorFieldMetadata parseIdField(Field field, VectorId id) {
        VectorDataType type = resolveType(field, id.type());
        return new VectorFieldMetadata(
                field,
                resolveName(field, id.name()),
                type,
                id.maxLength(),
                -1,
                null,
                true,
                id.autoId(),
                true,
                true);
    }

    private VectorFieldMetadata parseNormalField(Field field, VectorField vectorField) {
        VectorDataType type = resolveType(field, vectorField.type());
        int maxLength = vectorField.maxLength();
        if (maxLength < 0 && type == VectorDataType.VARCHAR) {
            maxLength = field.isAnnotationPresent(VectorEmbeddingText.class) ? 8192 : 512;
        }
        return new VectorFieldMetadata(
                field,
                resolveName(field, vectorField.name()),
                type,
                maxLength,
                vectorField.dimension(),
                vectorField.metricType(),
                false,
                false,
                vectorField.filterable(),
                vectorField.output());
    }

    private VectorIndexMetadata parseIndex(VectorIndex index, String defaultFieldName) {
        String fieldName = index.field();
        if (fieldName == null || fieldName.trim().isEmpty()) {
            fieldName = defaultFieldName;
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "@VectorIndex 必须指定 field，或者直接标注在字段上");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (String param : index.params()) {
            int separator = param.indexOf('=');
            if (separator <= 0 || separator == param.length() - 1) {
                throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                        "@VectorIndex params 格式必须是 key=value: " + param);
            }
            params.put(param.substring(0, separator).trim(), parseParamValue(param.substring(separator + 1).trim()));
        }
        String indexName = index.name();
        if (indexName == null || indexName.trim().isEmpty()) {
            indexName = "idx_" + toSnakeCase(fieldName);
        }
        return new VectorIndexMetadata(fieldName, indexName, index.type(), index.metricType(), params);
    }

    private Object parseParamValue(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException ignoredAgain) {
                return value;
            }
        }
    }

    private void validateEmbeddingTargets(Class<?> domainType, List<VectorFieldMetadata> fields,
                                          List<EmbeddingFieldMetadata> embeddingFields) {
        for (EmbeddingFieldMetadata embeddingField : embeddingFields) {
            boolean exists = fields.stream()
                    .anyMatch(field -> field.getJavaName().equals(embeddingField.getVectorField())
                            && field.getType() == VectorDataType.FLOAT_VECTOR);
            if (!exists) {
                throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                        "@VectorEmbeddingText 指向的向量字段不存在或不是 FLOAT_VECTOR: "
                                + domainType.getName() + "." + embeddingField.getVectorField());
            }
        }
    }

    private String resolveName(Field field, String configuredName) {
        if (configuredName != null && !configuredName.trim().isEmpty()) {
            return configuredName;
        }
        return toSnakeCase(field.getName());
    }

    private VectorDataType resolveType(Field field, VectorDataType configuredType) {
        if (configuredType != VectorDataType.AUTO) {
            return configuredType;
        }
        Class<?> type = field.getType();
        if (type == String.class) {
            return VectorDataType.VARCHAR;
        }
        if (type == Integer.class || type == int.class) {
            return VectorDataType.INT32;
        }
        if (type == Long.class || type == long.class) {
            return VectorDataType.INT64;
        }
        if (type == Float.class || type == float.class) {
            return VectorDataType.FLOAT;
        }
        if (type == Double.class || type == double.class) {
            return VectorDataType.DOUBLE;
        }
        if (type == Boolean.class || type == boolean.class) {
            return VectorDataType.BOOL;
        }
        if (List.class.isAssignableFrom(type)) {
            return VectorDataType.FLOAT_VECTOR;
        }
        throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                "无法自动推断字段类型，请显式指定 @VectorField(type=...): "
                        + field.getDeclaringClass().getName() + "." + field.getName());
    }

    private String toSnakeCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }
}
