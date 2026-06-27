package com.rongqi.vector.server.dto;

import com.rongqi.vector.core.FilterCondition;
import com.rongqi.vector.core.JsonToString;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HTTP 向量检索请求。
 *
 * <p>query 和 vector 二选一。query 会调用 EmbeddingProvider 生成向量；
 * vector 表示调用方已经生成好向量。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class VectorSearchRequest {
    private String domain;
    private String collection;
    private String query;
    private List<Float> vector;
    private Map<String, Object> filterObject = new LinkedHashMap<>();
    private Integer topK;
    private Integer candidateTopK;
    private Double minScore;
    private List<String> outputFields = new ArrayList<>();
    private List<FilterCondition> filters = new ArrayList<>();
    private VectorRankRequest rank;

    @Override
    public String toString() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("domain", domain);
        summary.put("collection", collection);
        summary.put("query", query);
        summary.put("vectorSize", vector == null ? 0 : vector.size());
        summary.put("filterFieldCount", filterObject == null ? 0 : filterObject.size());
        summary.put("topK", topK);
        summary.put("candidateTopK", candidateTopK);
        summary.put("minScore", minScore);
        summary.put("outputFields", outputFields);
        summary.put("filterConditionCount", filters == null ? 0 : filters.size());
        summary.put("rankEnabled", rank != null);
        return JsonToString.toJson(summary);
    }
}
