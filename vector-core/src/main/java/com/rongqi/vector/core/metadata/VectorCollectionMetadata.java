package com.rongqi.vector.core.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 一个 domain 类解析后的 collection 元数据。
 */
public class VectorCollectionMetadata {
    private final Class<?> domainType;
    private final String collectionName;
    private final String database;
    private final String description;
    private final boolean autoCreate;
    private final boolean autoCreateIndex;
    private final boolean validateSchema;
    private final boolean dynamicFieldEnabled;
    private final List<VectorFieldMetadata> fields;
    private final List<EmbeddingFieldMetadata> embeddingFields;
    private final List<VectorIndexMetadata> indexes;

    public VectorCollectionMetadata(Class<?> domainType, String collectionName, String database, String description,
                                    boolean autoCreate, boolean autoCreateIndex, boolean validateSchema,
                                    boolean dynamicFieldEnabled,
                                    List<VectorFieldMetadata> fields,
                                    List<EmbeddingFieldMetadata> embeddingFields,
                                    List<VectorIndexMetadata> indexes) {
        this.domainType = domainType;
        this.collectionName = collectionName;
        this.database = database;
        this.description = description;
        this.autoCreate = autoCreate;
        this.autoCreateIndex = autoCreateIndex;
        this.validateSchema = validateSchema;
        this.dynamicFieldEnabled = dynamicFieldEnabled;
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
        this.embeddingFields = Collections.unmodifiableList(new ArrayList<>(embeddingFields));
        this.indexes = Collections.unmodifiableList(new ArrayList<>(indexes));
    }

    public Class<?> getDomainType() {
        return domainType;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getDatabase() {
        return database;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public boolean isAutoCreateIndex() {
        return autoCreateIndex;
    }

    public boolean isValidateSchema() {
        return validateSchema;
    }

    public boolean isDynamicFieldEnabled() {
        return dynamicFieldEnabled;
    }

    public List<VectorFieldMetadata> getFields() {
        return fields;
    }

    public List<EmbeddingFieldMetadata> getEmbeddingFields() {
        return embeddingFields;
    }

    public List<VectorIndexMetadata> getIndexes() {
        return indexes;
    }

    public VectorFieldMetadata getIdField() {
        return fields.stream()
                .filter(VectorFieldMetadata::isId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("domain 缺少主键字段: " + domainType.getName()));
    }

    public Optional<VectorFieldMetadata> findFieldByJavaName(String javaName) {
        return fields.stream().filter(field -> field.getJavaName().equals(javaName)).findFirst();
    }

    public Optional<VectorFieldMetadata> findFieldByName(String fieldName) {
        Optional<VectorFieldMetadata> javaField = findFieldByJavaName(fieldName);
        if (javaField.isPresent()) {
            return javaField;
        }
        return fields.stream().filter(field -> field.getVectorName().equals(fieldName)).findFirst();
    }
}
