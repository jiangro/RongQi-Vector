package com.rongqi.vector.server.dto;

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
    private Double minScore;
    private List<String> outputFields = new ArrayList<>();
}
