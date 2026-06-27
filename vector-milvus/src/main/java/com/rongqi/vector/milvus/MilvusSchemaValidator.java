package com.rongqi.vector.milvus;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus 已有 Collection schema 校验器。
 *
 * <p>该类只负责比对期望 schema 和 Milvus 返回的 schema，不直接访问 Milvus，
 * 便于单元测试和后续扩展索引校验。</p>
 */
class MilvusSchemaValidator {

    /**
     * 校验已有 Collection 的字段 schema 是否符合框架定义。
     *
     * @param collection Collection 名称
     * @param expectedFields 框架期望字段列表
     * @param expectedDynamicFieldEnabled 框架期望的动态字段开关
     * @param actual Milvus describeCollection 返回值
     */
    void validate(String collection,
                  List<ExpectedField> expectedFields,
                  boolean expectedDynamicFieldEnabled,
                  DescribeCollectionResp actual) {
        if (actual == null || actual.getCollectionSchema() == null) {
            throw schemaException(collection, "无法读取 Milvus collection schema");
        }
        boolean actualDynamicFieldEnabled = actual.getEnableDynamicField() == null
                ? actual.getCollectionSchema().isEnableDynamicField()
                : actual.getEnableDynamicField();
        if (actualDynamicFieldEnabled != expectedDynamicFieldEnabled) {
            throw schemaException(collection,
                    "dynamicFieldEnabled 配置不一致, expected=" + expectedDynamicFieldEnabled
                            + ", actual=" + actualDynamicFieldEnabled);
        }

        Map<String, CreateCollectionReq.FieldSchema> actualFields = toFieldMap(actual);
        for (ExpectedField expectedField : expectedFields) {
            CreateCollectionReq.FieldSchema actualField = actualFields.get(expectedField.name);
            if (actualField == null) {
                throw schemaException(collection, "字段不存在: " + expectedField.name);
            }
            validateField(collection, expectedField, actualField);
        }
    }

    private void validateField(String collection,
                               ExpectedField expected,
                               CreateCollectionReq.FieldSchema actual) {
        if (actual.getDataType() != expected.dataType) {
            throw schemaException(collection,
                    "字段类型不一致: " + expected.name
                            + ", expected=" + expected.dataType
                            + ", actual=" + actual.getDataType());
        }
        if (boolValue(actual.getIsPrimaryKey()) != expected.primaryKey) {
            throw schemaException(collection,
                    "主键配置不一致: " + expected.name
                            + ", expected=" + expected.primaryKey
                            + ", actual=" + boolValue(actual.getIsPrimaryKey()));
        }
        if (boolValue(actual.getAutoID()) != expected.autoId) {
            throw schemaException(collection,
                    "autoId 配置不一致: " + expected.name
                            + ", expected=" + expected.autoId
                            + ", actual=" + boolValue(actual.getAutoID()));
        }
        if (expected.maxLength > 0 && intValue(actual.getMaxLength()) != expected.maxLength) {
            throw schemaException(collection,
                    "字符串最大长度不一致: " + expected.name
                            + ", expected=" + expected.maxLength
                            + ", actual=" + intValue(actual.getMaxLength()));
        }
        if (expected.dimension > 0 && intValue(actual.getDimension()) != expected.dimension) {
            throw schemaException(collection,
                    "向量维度不一致: " + expected.name
                            + ", expected=" + expected.dimension
                            + ", actual=" + intValue(actual.getDimension()));
        }
    }

    private Map<String, CreateCollectionReq.FieldSchema> toFieldMap(DescribeCollectionResp actual) {
        Map<String, CreateCollectionReq.FieldSchema> fields = new LinkedHashMap<>();
        for (CreateCollectionReq.FieldSchema field : actual.getCollectionSchema().getFieldSchemaList()) {
            fields.put(field.getName(), field);
        }
        return fields;
    }

    private boolean boolValue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private int intValue(Integer value) {
        return value == null ? -1 : value;
    }

    private VectorException schemaException(String collection, String message) {
        return new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                "Milvus collection schema 校验失败: " + collection + ", " + message);
    }

    /**
     * 框架期望的字段结构。
     */
    static class ExpectedField {
        private final String name;
        private final DataType dataType;
        private final boolean primaryKey;
        private final boolean autoId;
        private final int maxLength;
        private final int dimension;

        /**
         * 创建期望字段定义。
         *
         * @param name 字段名
         * @param dataType Milvus 字段类型
         * @param primaryKey 是否主键
         * @param autoId 主键是否自动生成
         * @param maxLength 字符串最大长度，非字符串字段传 -1
         * @param dimension 向量维度，非向量字段传 -1
         */
        ExpectedField(String name, DataType dataType, boolean primaryKey, boolean autoId,
                      int maxLength, int dimension) {
            this.name = name;
            this.dataType = dataType;
            this.primaryKey = primaryKey;
            this.autoId = autoId;
            this.maxLength = maxLength;
            this.dimension = dimension;
        }
    }
}
