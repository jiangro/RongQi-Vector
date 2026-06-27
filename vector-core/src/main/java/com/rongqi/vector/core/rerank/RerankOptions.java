package com.rongqi.vector.core.rerank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rerank 调用参数。
 *
 * <p>用于指定 provider、model 以及厂商或自定义服务需要的附加属性。</p>
 */
public class RerankOptions {
    private final String provider;
    private final String model;
    private final Map<String, Object> attributes;

    private RerankOptions(Builder builder) {
        this.provider = trimToNull(builder.provider);
        this.model = trimToNull(builder.model);
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

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * RerankOptions 构建器。
     */
    public static class Builder {
        private String provider;
        private String model;
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        /**
         * 设置 RerankProvider 名称。
         */
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        /**
         * 设置 rerank 模型名称。
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 添加扩展属性。
         */
        public Builder attribute(String key, Object value) {
            if (key != null && !key.trim().isEmpty()) {
                this.attributes.put(key.trim(), value);
            }
            return this;
        }

        public RerankOptions build() {
            return new RerankOptions(this);
        }
    }
}
