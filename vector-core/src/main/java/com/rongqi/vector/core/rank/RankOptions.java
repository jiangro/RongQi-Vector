package com.rongqi.vector.core.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 搜索结果二次排序配置。
 *
 * <p>第一阶段支持字段加权排序，同时预留 profile 和 rerank provider 字段，方便后续按业务选择模型重排。</p>
 */
public class RankOptions {
    private final String profile;
    private final List<FieldBoost> fieldBoosts;
    private final String rerankProvider;
    private final String rerankModel;
    private final String rerankTextField;

    private RankOptions(Builder builder) {
        this.profile = trimToNull(builder.profile);
        this.fieldBoosts = Collections.unmodifiableList(new ArrayList<>(builder.fieldBoosts));
        this.rerankProvider = trimToNull(builder.rerankProvider);
        this.rerankModel = trimToNull(builder.rerankModel);
        this.rerankTextField = trimToNull(builder.rerankTextField);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProfile() {
        return profile;
    }

    public List<FieldBoost> getFieldBoosts() {
        return fieldBoosts;
    }

    public String getRerankProvider() {
        return rerankProvider;
    }

    public String getRerankModel() {
        return rerankModel;
    }

    public String getRerankTextField() {
        return rerankTextField;
    }

    /**
     * 是否配置了字段加权排序。
     */
    public boolean hasFieldBoosts() {
        return !fieldBoosts.isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * RankOptions 构建器。
     */
    public static class Builder {
        private String profile;
        private final List<FieldBoost> fieldBoosts = new ArrayList<>();
        private String rerankProvider;
        private String rerankModel;
        private String rerankTextField;

        /**
         * 设置业务排序配置名称，后续可用于从服务端配置中解析不同业务的 rerank 策略。
         */
        public Builder profile(String profile) {
            this.profile = profile;
            return this;
        }

        /**
         * 添加字段加权排序规则。
         */
        public Builder fieldBoost(String field, double weight) {
            this.fieldBoosts.add(new FieldBoost(field, weight));
            return this;
        }

        /**
         * 设置本次搜索使用的 RerankProvider 名称。
         */
        public Builder rerankProvider(String rerankProvider) {
            this.rerankProvider = rerankProvider;
            return this;
        }

        /**
         * 设置本次搜索使用的 rerank 模型名称。
         */
        public Builder rerankModel(String rerankModel) {
            this.rerankModel = rerankModel;
            return this;
        }

        /**
         * 设置传给 rerank 模型的正文文本字段名。
         */
        public Builder rerankTextField(String rerankTextField) {
            this.rerankTextField = rerankTextField;
            return this;
        }

        public RankOptions build() {
            return new RankOptions(this);
        }
    }
}
