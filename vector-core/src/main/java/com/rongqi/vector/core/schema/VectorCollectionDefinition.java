package com.rongqi.vector.core.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 或配置方式定义的 Collection Schema。
 *
 * <p>这个模型的能力应和 @VectorCollection + @VectorField + @VectorIndex 保持一致。</p>
 */
public class VectorCollectionDefinition {
    private String database;
    private String collection;
    private String description;
    private boolean autoCreate = true;
    private boolean autoCreateIndex = true;
    private boolean validateSchema = true;
    private boolean dynamicFieldEnabled = false;
    private List<VectorFieldDefinition> fields = new ArrayList<>();
    private List<VectorIndexDefinition> indexes = new ArrayList<>();
    private List<VectorEmbeddingDefinition> embeddings = new ArrayList<>();

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    public boolean isAutoCreateIndex() {
        return autoCreateIndex;
    }

    public void setAutoCreateIndex(boolean autoCreateIndex) {
        this.autoCreateIndex = autoCreateIndex;
    }

    public boolean isValidateSchema() {
        return validateSchema;
    }

    public void setValidateSchema(boolean validateSchema) {
        this.validateSchema = validateSchema;
    }

    public boolean isDynamicFieldEnabled() {
        return dynamicFieldEnabled;
    }

    public void setDynamicFieldEnabled(boolean dynamicFieldEnabled) {
        this.dynamicFieldEnabled = dynamicFieldEnabled;
    }

    public List<VectorFieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<VectorFieldDefinition> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    public List<VectorIndexDefinition> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<VectorIndexDefinition> indexes) {
        this.indexes = indexes == null ? new ArrayList<>() : indexes;
    }

    public List<VectorEmbeddingDefinition> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<VectorEmbeddingDefinition> embeddings) {
        this.embeddings = embeddings == null ? new ArrayList<>() : embeddings;
    }
}
