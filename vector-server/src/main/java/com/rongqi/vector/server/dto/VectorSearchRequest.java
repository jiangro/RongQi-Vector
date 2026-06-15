package com.rongqi.vector.server.dto;

import com.rongqi.vector.core.FilterCondition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * HTTP 向量检索请求。
 *
 * <p>query 和 vector 二选一。query 会调用 EmbeddingProvider 生成向量；
 * vector 表示调用方已经生成好向量。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class VectorSearchRequest {
    @ToString.Include
    private String domain;
    @ToString.Include
    private String collection;
    @ToString.Include
    private String query;
    private List<Float> vector;
    private Map<String, Object> filterObject = new LinkedHashMap<>();
    @ToString.Include
    private Integer topK;
    @ToString.Include
    private Double minScore;
    @ToString.Include
    private List<String> outputFields = new ArrayList<>();
    private List<FilterCondition> filters = new ArrayList<>();

    @ToString.Include(name = "vectorSize")
    private int vectorSize() {
        return vector == null ? 0 : vector.size();
    }

    @ToString.Include(name = "filterFieldCount")
    private int filterFieldCount() {
        return filterObject == null ? 0 : filterObject.size();
    }

    @ToString.Include(name = "filterConditionCount")
    private int filterConditionCount() {
        return filters == null ? 0 : filters.size();
    }
}
