package com.rongqi.vector.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 删除操作选项。
 *
 * <p>用于表达大于、小于、列表包含、列表排除和模糊匹配等复杂删除条件。</p>
 */
public class DeleteOptions {
    private final List<FilterCondition> filterConditions;

    private DeleteOptions(Builder builder) {
        this.filterConditions = Collections.unmodifiableList(new ArrayList<>(builder.filterConditions));
    }

    /**
     * 创建删除选项构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回显式过滤条件列表。
     *
     * @return 过滤条件列表
     */
    public List<FilterCondition> getFilterConditions() {
        return filterConditions;
    }

    @Override
    public String toString() {
        return JsonToString.toJson(this);
    }

    /**
     * DeleteOptions 构建器。
     */
    public static class Builder {
        private final List<FilterCondition> filterConditions = new ArrayList<>();

        /**
         * 添加一个通用过滤条件。
         *
         * @param field 字段名，注解模式可传 Java 字段名或 Milvus 字段名，HTTP collection 模式传 schema 字段名
         * @param operator 过滤操作符，为空时按等于处理
         * @param value 过滤值，IN 和 NOT_IN 必须传集合
         * @return 构建器
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

        /**
         * 构建删除选项。
         *
         * @return 删除选项
         */
        public DeleteOptions build() {
            return new DeleteOptions(this);
        }
    }
}
