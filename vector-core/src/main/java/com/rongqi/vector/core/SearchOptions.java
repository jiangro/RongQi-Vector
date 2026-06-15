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
    private final List<FilterCondition> filterConditions;
    private final Map<String, Object> searchParams;

    private SearchOptions(Builder builder) {
        this.topK = builder.topK;
        this.minScore = builder.minScore;
        this.outputFields = Collections.unmodifiableList(new ArrayList<>(builder.outputFields));
        this.filterConditions = Collections.unmodifiableList(new ArrayList<>(builder.filterConditions));
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

    /**
     * 返回显式过滤条件列表。
     */
    public List<FilterCondition> getFilterConditions() {
        return filterConditions;
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
        private final List<FilterCondition> filterConditions = new ArrayList<>();
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

        /**
         * 添加一个通用过滤条件。
         *
         * @param field 字段名，注解模式可传 Java 字段名或 Milvus 字段名，HTTP collection 模式传 schema 字段名
         * @param operator 过滤操作符，为空时按等于处理
         * @param value 过滤值，IN 和 NOT_IN 必须传集合
         */
        public Builder filter(String field, FilterOperator operator, Object value) {
            this.filterConditions.add(FilterCondition.of(field, operator, value));
            return this;
        }

        /**
         * 添加等于过滤条件。
         */
        public Builder eq(String field, Object value) {
            return filter(field, FilterOperator.EQ, value);
        }

        /**
         * 添加不等于过滤条件。
         */
        public Builder ne(String field, Object value) {
            return filter(field, FilterOperator.NE, value);
        }

        /**
         * 添加大于过滤条件。
         */
        public Builder gt(String field, Object value) {
            return filter(field, FilterOperator.GT, value);
        }

        /**
         * 添加大于等于过滤条件。
         */
        public Builder gte(String field, Object value) {
            return filter(field, FilterOperator.GTE, value);
        }

        /**
         * 添加小于过滤条件。
         */
        public Builder lt(String field, Object value) {
            return filter(field, FilterOperator.LT, value);
        }

        /**
         * 添加小于等于过滤条件。
         */
        public Builder lte(String field, Object value) {
            return filter(field, FilterOperator.LTE, value);
        }

        /**
         * 添加字段值在集合中的过滤条件。
         */
        public Builder in(String field, Object value) {
            return filter(field, FilterOperator.IN, value);
        }

        /**
         * 添加字段值不在集合中的过滤条件。
         */
        public Builder notIn(String field, Object value) {
            return filter(field, FilterOperator.NOT_IN, value);
        }

        /**
         * 添加字符串模糊匹配过滤条件。
         */
        public Builder like(String field, String value) {
            return filter(field, FilterOperator.LIKE, value);
        }

        public Builder searchParam(String key, Object value) {
            this.searchParams.put(key, value);
            return this;
        }

        public SearchOptions build() {
            return new SearchOptions(this);
        }
    }

    @Override
    public String toString() {
        return JsonToString.toJson(this);
    }
}
