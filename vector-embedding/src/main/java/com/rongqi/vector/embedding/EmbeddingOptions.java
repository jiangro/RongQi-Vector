package com.rongqi.vector.embedding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Embedding 调用参数。
 *
 * <p>不同模型可能需要不同参数，因此保留 attributes 扩展字段。</p>
 */
public class EmbeddingOptions {
    private final String provider;
    private final String model;
    private final Map<String, Object> attributes;

    private EmbeddingOptions(Builder builder) {
        this.provider = builder.provider;
        this.model = builder.model;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * EmbeddingOptions 构建器。
     */
    public static class Builder {
        private String provider;
        private String model;
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public EmbeddingOptions build() {
            return new EmbeddingOptions(this);
        }
    }
}

