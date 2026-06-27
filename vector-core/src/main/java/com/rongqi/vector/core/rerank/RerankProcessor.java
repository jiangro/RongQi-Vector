package com.rongqi.vector.core.rerank;

import com.rongqi.vector.core.SearchHit;
import com.rongqi.vector.core.SearchOptions;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.rank.RankOptions;
import com.rongqi.vector.core.rank.Ranker;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rerank 搜索结果处理器。
 *
 * <p>该类负责把第一阶段向量召回结果转换成 rerank 文档，调用 RerankProvider 后再按新分数排序。
 * 没有显式配置 rerank 时，会自动回退到原来的字段加权 Ranker。</p>
 */
public class RerankProcessor {
    private static final String DEFAULT_TEXT_FIELD = "text";

    private final RerankProviderRegistry providerRegistry;
    private final String defaultProvider;
    private final Ranker ranker;

    /**
     * 创建 rerank 处理器。
     *
     * @param providerRegistry RerankProvider 注册表
     * @param defaultProvider 默认 provider 名称，允许为空
     */
    public RerankProcessor(RerankProviderRegistry providerRegistry, String defaultProvider) {
        this.providerRegistry = providerRegistry;
        this.defaultProvider = trimToNull(defaultProvider);
        this.ranker = new Ranker();
    }

    /**
     * 对搜索结果进行 rerank 或普通 rank。
     *
     * @param query 用户搜索文本，只有文本搜索才有 query；向量搜索不会启用 rerank
     * @param hits Milvus 返回的候选结果
     * @param options 搜索配置
     * @param <T> 命中实体类型
     * @return 最终排序后的结果
     */
    public <T> List<SearchHit<T>> rank(String query, List<SearchHit<T>> hits, SearchOptions options) {
        SearchOptions actualOptions = options == null ? SearchOptions.topK(10) : options;
        RankOptions rankOptions = actualOptions.getRankOptions();
        if (!shouldRerank(query, rankOptions)) {
            return ranker.rank(hits, actualOptions);
        }
        String providerName = resolveProviderName(rankOptions);
        RerankProvider provider = requireProvider(providerName);
        List<RerankDocument> documents = toDocuments(hits, resolveTextField(rankOptions));
        if (documents.isEmpty()) {
            return ranker.rank(hits, actualOptions);
        }
        List<RerankResult> results = provider.rerank(query, documents, RerankOptions.builder()
                .provider(providerName)
                .model(rankOptions.getRerankModel())
                .build());
        List<SearchHit<T>> reranked = applyRerankScores(hits, results);
        return ranker.rank(reranked, actualOptions);
    }

    private boolean shouldRerank(String query, RankOptions rankOptions) {
        if (query == null || query.trim().isEmpty() || rankOptions == null) {
            return false;
        }
        return rankOptions.getRerankProvider() != null
                || rankOptions.getRerankModel() != null
                || rankOptions.getRerankTextField() != null;
    }

    private String resolveProviderName(RankOptions rankOptions) {
        if (rankOptions.getRerankProvider() != null) {
            return rankOptions.getRerankProvider();
        }
        if (defaultProvider != null) {
            return defaultProvider;
        }
        throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                "已配置 rerank，但没有指定 rerankProvider，也没有配置默认 provider");
    }

    private RerankProvider requireProvider(String providerName) {
        try {
            return providerRegistry.require(providerName);
        } catch (IllegalArgumentException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID, exception.getMessage(), exception);
        }
    }

    private String resolveTextField(RankOptions rankOptions) {
        if (rankOptions.getRerankTextField() != null) {
            return rankOptions.getRerankTextField();
        }
        return DEFAULT_TEXT_FIELD;
    }

    private <T> List<RerankDocument> toDocuments(List<SearchHit<T>> hits, String textField) {
        List<RerankDocument> documents = new ArrayList<>();
        if (hits == null || hits.isEmpty()) {
            return documents;
        }
        for (int i = 0; i < hits.size(); i++) {
            SearchHit<T> hit = hits.get(i);
            Object text = readValue(hit.getEntity(), textField);
            if (text == null || String.valueOf(text).trim().isEmpty()) {
                throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                        "rerankTextField 指向的字段没有返回文本: " + textField
                                + "，请确认该字段存在并包含在 outputFields 中");
            }
            documents.add(new RerankDocument(i, null, String.valueOf(text), hit.getScore()));
        }
        return documents;
    }

    private <T> List<SearchHit<T>> applyRerankScores(List<SearchHit<T>> hits, List<RerankResult> results) {
        Map<Integer, Double> scoreByIndex = new HashMap<>();
        if (results != null) {
            for (RerankResult result : results) {
                scoreByIndex.put(result.getIndex(), result.getScore());
            }
        }
        List<SearchHit<T>> ranked = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            SearchHit<T> hit = hits.get(i);
            Double rerankScore = scoreByIndex.get(i);
            if (rerankScore == null) {
                rerankScore = hit.getRankScore() == null ? hit.getScore() : hit.getRankScore();
            }
            ranked.add(new SearchHit<>(hit.getScore(), rerankScore, hit.getEntity()));
        }
        ranked.sort(Comparator.comparing(SearchHit<T>::getRankScore).reversed());
        return ranked;
    }

    /**
     * 读取 Map、JavaBean getter 或普通字段中的值，兼容 HTTP collection 和注解实体两种结果。
     */
    private Object readValue(Object entity, String fieldName) {
        if (entity == null || fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }
        if (entity instanceof Map) {
            return ((Map<?, ?>) entity).get(fieldName);
        }
        Object getterValue = readGetter(entity, fieldName);
        if (getterValue != null) {
            return getterValue;
        }
        return readField(entity, fieldName);
    }

    private Object readGetter(Object entity, String fieldName) {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String[] methodNames = new String[]{"get" + suffix, "is" + suffix};
        for (String methodName : methodNames) {
            try {
                Method method = entity.getClass().getMethod(methodName);
                return method.invoke(entity);
            } catch (ReflectiveOperationException ignored) {
                // 当前实体没有该 getter 时继续尝试普通字段。
            }
        }
        return null;
    }

    private Object readField(Object entity, String fieldName) {
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(entity);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
