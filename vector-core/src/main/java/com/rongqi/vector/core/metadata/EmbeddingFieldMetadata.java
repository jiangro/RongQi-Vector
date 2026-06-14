package com.rongqi.vector.core.metadata;

/**
 * 文本字段和向量字段之间的 embedding 映射关系。
 */
public class EmbeddingFieldMetadata {
    private final String textField;
    private final String vectorField;
    private final String provider;
    private final String model;

    public EmbeddingFieldMetadata(String textField, String vectorField, String provider, String model) {
        this.textField = textField;
        this.vectorField = vectorField;
        this.provider = provider;
        this.model = model;
    }

    public String getTextField() {
        return textField;
    }

    public String getVectorField() {
        return vectorField;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }
}

