package com.rongqi.vector.core.schema;

/**
 * HTTP 或配置方式定义的文本字段到向量字段的 Embedding 映射。
 */
public class VectorEmbeddingDefinition {
    private String textField;
    private String vectorField;
    private String provider;
    private String model;

    public String getTextField() {
        return textField;
    }

    public void setTextField(String textField) {
        this.textField = textField;
    }

    public String getVectorField() {
        return vectorField;
    }

    public void setVectorField(String vectorField) {
        this.vectorField = vectorField;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

