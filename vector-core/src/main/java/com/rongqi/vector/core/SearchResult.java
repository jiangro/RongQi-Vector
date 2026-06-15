package com.rongqi.vector.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 向量检索结果集合。
 *
 * @param <T> 业务 domain 类型
 */
public class SearchResult<T> {
    private final List<SearchHit<T>> hits;

    public SearchResult(List<SearchHit<T>> hits) {
        this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
    }

    public static <T> SearchResult<T> empty() {
        return new SearchResult<>(Collections.emptyList());
    }

    public List<SearchHit<T>> getHits() {
        return hits;
    }

    @Override
    public String toString() {
        return JsonToString.toJson(this);
    }
}
