package com.rongqi.vector.milvus;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import com.rongqi.vector.core.schema.VectorEmbeddingDefinition;
import com.rongqi.vector.core.schema.VectorFieldDefinition;
import com.rongqi.vector.embedding.EmbeddingOptions;
import com.rongqi.vector.embedding.EmbeddingProvider;
import com.rongqi.vector.embedding.EmbeddingProviderRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * HTTP collection 模式的批量 Embedding 填充器。
 *
 * <p>该类直接操作 Map 数据，字段名使用 /collections/ensure 注册的 schema 字段名。</p>
 */
class GenericEmbeddingBatchFiller {
    private static final int DEFAULT_BATCH_SIZE = 32;

    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final String defaultEmbeddingProvider;
    private final int batchSize;

    /**
     * 创建批量填充器。
     *
     * @param embeddingProviderRegistry EmbeddingProvider 注册表
     * @param defaultEmbeddingProvider 默认 provider 名称
     */
    GenericEmbeddingBatchFiller(EmbeddingProviderRegistry embeddingProviderRegistry, String defaultEmbeddingProvider) {
        this(embeddingProviderRegistry, defaultEmbeddingProvider, DEFAULT_BATCH_SIZE);
    }

    /**
     * 创建批量填充器。
     *
     * @param embeddingProviderRegistry EmbeddingProvider 注册表
     * @param defaultEmbeddingProvider 默认 provider 名称
     * @param batchSize 每次调用 provider 的文本数量，非法值会回退为 32
     */
    GenericEmbeddingBatchFiller(EmbeddingProviderRegistry embeddingProviderRegistry,
                                String defaultEmbeddingProvider,
                                int batchSize) {
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.defaultEmbeddingProvider = defaultEmbeddingProvider == null || defaultEmbeddingProvider.trim().isEmpty()
                ? "noop"
                : defaultEmbeddingProvider;
        this.batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }

    /**
     * 为缺少向量字段的 Map 数据批量生成 Embedding。
     *
     * @param definition collection schema 定义
     * @param items 待写入数据
     */
    void fillMissingEmbeddings(VectorCollectionDefinition definition, Collection<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (VectorEmbeddingDefinition embedding : definition.getEmbeddings()) {
            fillOneEmbeddingField(definition, embedding, items);
        }
    }

    private void fillOneEmbeddingField(VectorCollectionDefinition definition,
                                       VectorEmbeddingDefinition embedding,
                                       Collection<Map<String, Object>> items) {
        VectorFieldDefinition vectorField = requireField(definition, embedding.getVectorField());
        List<Map<String, Object>> pendingItems = new ArrayList<>();
        List<String> pendingTexts = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Object currentVector = item.get(embedding.getVectorField());
            if (currentVector != null) {
                validateVectorDimension(vectorField, currentVector);
                continue;
            }
            Object text = item.get(embedding.getTextField());
            if (text == null || String.valueOf(text).trim().isEmpty()) {
                continue;
            }
            pendingItems.add(item);
            pendingTexts.add(String.valueOf(text));
        }
        if (pendingTexts.isEmpty()) {
            return;
        }

        String providerName = resolveProviderName(embedding);
        EmbeddingProvider provider = embeddingProviderRegistry.require(providerName);
        EmbeddingOptions options = EmbeddingOptions.builder()
                .provider(providerName)
                .model(embedding.getModel())
                .build();
        for (int from = 0; from < pendingTexts.size(); from += batchSize) {
            int to = Math.min(from + batchSize, pendingTexts.size());
            List<String> batchTexts = pendingTexts.subList(from, to);
            List<List<Float>> vectors = provider.embed(batchTexts, options);
            validateEmbeddingResultSize(vectorField, batchTexts, vectors);
            for (int i = 0; i < batchTexts.size(); i++) {
                List<Float> vector = vectors.get(i);
                validateVectorDimension(vectorField, vector);
                pendingItems.get(from + i).put(embedding.getVectorField(), vector);
            }
        }
    }

    private VectorFieldDefinition requireField(VectorCollectionDefinition definition, String fieldName) {
        return definition.getFields().stream()
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "字段不存在: " + fieldName + ", collection=" + definition.getCollection()));
    }

    private String resolveProviderName(VectorEmbeddingDefinition embedding) {
        if (embedding != null && embedding.getProvider() != null && !embedding.getProvider().trim().isEmpty()) {
            return embedding.getProvider();
        }
        return defaultEmbeddingProvider;
    }

    private void validateEmbeddingResultSize(VectorFieldDefinition field, List<String> texts, List<List<Float>> vectors) {
        if (vectors == null || vectors.size() != texts.size()) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                    "Embedding 返回数量不匹配: " + field.getName()
                            + ", expected=" + texts.size()
                            + ", actual=" + (vectors == null ? 0 : vectors.size()));
        }
    }

    @SuppressWarnings("unchecked")
    private void validateVectorDimension(VectorFieldDefinition field, Object vectorValue) {
        if (!(vectorValue instanceof List)) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "向量字段必须是 List<Float>: " + field.getName());
        }
        validateVectorDimension(field, (List<Float>) vectorValue);
    }

    private void validateVectorDimension(VectorFieldDefinition field, List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID, "向量不能为空: " + field.getName());
        }
        if (field.getDimension() > 0 && vector.size() != field.getDimension()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "向量维度不匹配: " + field.getName()
                            + ", expected=" + field.getDimension() + ", actual=" + vector.size());
        }
    }
}
