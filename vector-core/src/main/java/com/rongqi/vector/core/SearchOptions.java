package com.rongqi.vector.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量检索选项。
 *
 * <p>业务方可以只使用 {@link #topK(int)}，也可以通过 builder 设置高级参数。</p>
 */
public class SearchOptions {
    private final int topK;
    private final Double minScore;
    private final List<String> outputFields;
    private final Map<String, Object> searchParams;

    private SearchOptions(Builder builder) {
        this.topK = builder.topK;
        this.minScore = builder.minScore;
        this.outputFields = Collections.unmodifiableList(new ArrayList<>(builder.outputFields));
        this.searchParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.searchParams));
    }

    public static SearchOptions topK(int topK) {
        return builder().topK(topK).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getTopK() {
        return topK;
    }

    public Double getMinScore() {
        return minScore;
    }

    public List<String> getOutputFields() {
        return outputFields;
    }

    public Map<String, Object> getSearchParams() {
        return searchParams;
    }

    /**
     * SearchOptions 构建器，避免构造方法参数过多。
     */
    public static class Builder {
        private int topK = 10;
        private Double minScore;
        private final List<String> outputFields = new ArrayList<>();
        private final Map<String, Object> searchParams = new LinkedHashMap<>();

        public Builder topK(int topK) {
            this.topK = Math.max(1, topK);
            return this;
        }

        public Builder minScore(Double minScore) {
            this.minScore = minScore;
            return this;
        }

        public Builder outputFields(String... fields) {
            Collections.addAll(this.outputFields, fields);
            return this;
        }

        public Builder searchParam(String key, Object value) {
            this.searchParams.put(key, value);
            return this;
        }

        public SearchOptions build() {
            return new SearchOptions(this);
        }
    }
}

