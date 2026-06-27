package com.rongqi.vector.core;

import com.rongqi.vector.core.rank.RankOptions;
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
    private final int candidateTopK;
    private final Double minScore;
    private final List<String> outputFields;
    private final List<FilterCondition> filterConditions;
    private final Map<String, Object> searchParams;
    private final RankOptions rankOptions;

    private SearchOptions(Builder builder) {
        this.topK = builder.topK;
        this.candidateTopK = Math.max(builder.candidateTopK, builder.topK);
        this.minScore = builder.minScore;
        this.outputFields = Collections.unmodifiableList(new ArrayList<>(builder.outputFields));
        this.filterConditions = Collections.unmodifiableList(new ArrayList<>(builder.filterConditions));
        this.searchParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.searchParams));
        this.rankOptions = builder.rankOptions;
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

    /**
     * 返回第一阶段向量召回候选数量。
     *
     * <p>未启用 rank/rerank 时通常等于 topK；启用二次排序时可以设置得更大，让排序有更多候选。</p>
     */
    public int getCandidateTopK() {
        return candidateTopK;
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
     * 返回二次排序配置，空表示不启用 rank/rerank。
     */
    public RankOptions getRankOptions() {
        return rankOptions;
    }

    /**
     * SearchOptions 构建器，避免构造方法参数过多。
     */
    public static class Builder {
        private int topK = 10;
        private int candidateTopK = 10;
        private Double minScore;
        private final List<String> outputFields = new ArrayList<>();
        private final List<FilterCondition> filterConditions = new ArrayList<>();
        private final Map<String, Object> searchParams = new LinkedHashMap<>();
        private RankOptions rankOptions;

        public Builder topK(int topK) {
            this.topK = Math.max(1, topK);
            return this;
        }

        /**
         * 设置第一阶段向量召回候选数量。
         *
         * <p>该值小于 topK 时会自动提升到 topK，避免最终结果数量不足。</p>
         */
        public Builder candidateTopK(int candidateTopK) {
            this.candidateTopK = Math.max(1, candidateTopK);
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

        /**
         * 设置搜索结果二次排序配置。
         */
        public Builder rank(RankOptions rankOptions) {
            this.rankOptions = rankOptions;
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
