package com.rongqi.vector.milvus;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.EmbeddingFieldMetadata;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import com.rongqi.vector.embedding.EmbeddingOptions;
import com.rongqi.vector.embedding.EmbeddingProvider;
import com.rongqi.vector.embedding.EmbeddingProviderRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * domain 模式的批量 Embedding 填充器。
 *
 * <p>写入多条数据时，同一个文本字段会合并成一次 provider 调用，避免每条数据都发起一次网络请求。</p>
 */
class DomainEmbeddingBatchFiller {
    private static final int DEFAULT_BATCH_SIZE = 32;

    private final DomainValueAccessor accessor;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final String defaultEmbeddingProvider;
    private final int batchSize;

    /**
     * 创建批量填充器。
     *
     * @param accessor domain 字段访问器
     * @param embeddingProviderRegistry EmbeddingProvider 注册表
     * @param defaultEmbeddingProvider 默认 provider 名称
     */
    DomainEmbeddingBatchFiller(DomainValueAccessor accessor,
                               EmbeddingProviderRegistry embeddingProviderRegistry,
                               String defaultEmbeddingProvider) {
        this(accessor, embeddingProviderRegistry, defaultEmbeddingProvider, DEFAULT_BATCH_SIZE);
    }

    /**
     * 创建批量填充器。
     *
     * @param accessor domain 字段访问器
     * @param embeddingProviderRegistry EmbeddingProvider 注册表
     * @param defaultEmbeddingProvider 默认 provider 名称
     * @param batchSize 每次调用 provider 的文本数量，非法值会回退为 32
     */
    DomainEmbeddingBatchFiller(DomainValueAccessor accessor,
                               EmbeddingProviderRegistry embeddingProviderRegistry,
                               String defaultEmbeddingProvider,
                               int batchSize) {
        this.accessor = accessor;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.defaultEmbeddingProvider = defaultEmbeddingProvider == null || defaultEmbeddingProvider.trim().isEmpty()
                ? "noop"
                : defaultEmbeddingProvider;
        this.batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }

    /**
     * 为缺失向量的实体批量生成 Embedding。
     *
     * @param metadata domain 元数据
     * @param entities 待写入实体
     * @param <T> 实体类型
     */
    <T> void fillMissingEmbeddings(VectorCollectionMetadata metadata, Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (EmbeddingFieldMetadata embeddingField : metadata.getEmbeddingFields()) {
            fillOneEmbeddingField(metadata, embeddingField, entities);
        }
    }

    private <T> void fillOneEmbeddingField(VectorCollectionMetadata metadata,
                                           EmbeddingFieldMetadata embeddingField,
                                           Collection<T> entities) {
        VectorFieldMetadata textField = metadata.findFieldByJavaName(embeddingField.getTextField())
                .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "找不到 embedding 文本字段: " + embeddingField.getTextField()));
        VectorFieldMetadata vectorField = metadata.findFieldByJavaName(embeddingField.getVectorField())
                .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "找不到 embedding 向量字段: " + embeddingField.getVectorField()));

        List<T> pendingEntities = new ArrayList<>();
        List<String> pendingTexts = new ArrayList<>();
        for (T entity : entities) {
            Object currentVector = accessor.get(entity, vectorField);
            if (currentVector != null) {
                validateVectorDimension(vectorField, currentVector);
                continue;
            }
            Object text = accessor.get(entity, textField);
            if (text == null || String.valueOf(text).trim().isEmpty()) {
                continue;
            }
            pendingEntities.add(entity);
            pendingTexts.add(String.valueOf(text));
        }
        if (pendingTexts.isEmpty()) {
            return;
        }

        String providerName = resolveProviderName(embeddingField);
        EmbeddingProvider provider = embeddingProviderRegistry.require(providerName);
        EmbeddingOptions options = EmbeddingOptions.builder()
                .provider(providerName)
                .model(embeddingField.getModel())
                .build();
        for (int from = 0; from < pendingTexts.size(); from += batchSize) {
            int to = Math.min(from + batchSize, pendingTexts.size());
            List<String> batchTexts = pendingTexts.subList(from, to);
            List<List<Float>> vectors = provider.embed(batchTexts, options);
            validateEmbeddingResultSize(vectorField, batchTexts, vectors);
            for (int i = 0; i < batchTexts.size(); i++) {
                List<Float> vector = vectors.get(i);
                validateVectorDimension(vectorField, vector);
                accessor.set(pendingEntities.get(from + i), vectorField, vector);
            }
        }
    }

    private String resolveProviderName(EmbeddingFieldMetadata embeddingField) {
        if (embeddingField != null && embeddingField.getProvider() != null
                && !embeddingField.getProvider().trim().isEmpty()) {
            return embeddingField.getProvider();
        }
        return defaultEmbeddingProvider;
    }

    private void validateEmbeddingResultSize(VectorFieldMetadata field, List<String> texts, List<List<Float>> vectors) {
        if (vectors == null || vectors.size() != texts.size()) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                    "Embedding 返回数量不匹配: " + field.getJavaName()
                            + ", expected=" + texts.size()
                            + ", actual=" + (vectors == null ? 0 : vectors.size()));
        }
    }

    @SuppressWarnings("unchecked")
    private void validateVectorDimension(VectorFieldMetadata field, Object vectorValue) {
        if (!(vectorValue instanceof List)) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "向量字段必须是 List<Float>: " + field.getJavaName());
        }
        validateVectorDimension(field, (List<Float>) vectorValue);
    }

    private void validateVectorDimension(VectorFieldMetadata field, List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "向量不能为空: " + field.getJavaName());
        }
        if (field.getDimension() > 0 && vector.size() != field.getDimension()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "向量维度不匹配: " + field.getJavaName()
                            + ", expected=" + field.getDimension() + ", actual=" + vector.size());
        }
    }
}
