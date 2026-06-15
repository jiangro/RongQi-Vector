package com.rongqi.vector.milvus;

import com.google.gson.JsonObject;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.core.DeleteResult;
import com.rongqi.vector.core.SearchHit;
import com.rongqi.vector.core.SearchOptions;
import com.rongqi.vector.core.SearchResult;
import com.rongqi.vector.core.UpsertResult;
import com.rongqi.vector.core.VectorDiagnosis;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.VectorTemplate;
import com.rongqi.vector.core.metadata.DomainMetadataParser;
import com.rongqi.vector.core.metadata.EmbeddingFieldMetadata;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import com.rongqi.vector.embedding.EmbeddingOptions;
import com.rongqi.vector.embedding.EmbeddingProvider;
import com.rongqi.vector.embedding.EmbeddingProviderRegistry;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Milvus 的 VectorTemplate 实现。
 *
 * <p>该类是业务方最常用的入口，负责串联 domain 注解解析、collection 创建、
 * embedding 生成、Milvus 写入、检索和删除。</p>
 */
public class MilvusVectorTemplate implements VectorTemplate, AutoCloseable {
    private final MilvusClientProperties properties;
    private final DomainMetadataParser metadataParser;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final String defaultEmbeddingProvider;
    private final MilvusClientFactory clientFactory;
    private final MilvusCollectionManager collectionManager;
    private final DomainValueAccessor accessor;
    private final MilvusEntityMapper entityMapper;
    private final FilterExpressionBuilder filterExpressionBuilder;
    private final MilvusTypeMapper typeMapper;

    public MilvusVectorTemplate(MilvusClientProperties properties,
                                DomainMetadataParser metadataParser,
                                EmbeddingProviderRegistry embeddingProviderRegistry) {
        this(properties, metadataParser, embeddingProviderRegistry, "noop");
    }

    public MilvusVectorTemplate(MilvusClientProperties properties,
                                DomainMetadataParser metadataParser,
                                EmbeddingProviderRegistry embeddingProviderRegistry,
                                String defaultEmbeddingProvider) {
        this.properties = properties;
        this.metadataParser = metadataParser;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.defaultEmbeddingProvider = defaultEmbeddingProvider == null || defaultEmbeddingProvider.trim().isEmpty()
                ? "noop"
                : defaultEmbeddingProvider;
        this.clientFactory = new MilvusClientFactory(properties);
        this.typeMapper = new MilvusTypeMapper();
        this.collectionManager = new MilvusCollectionManager(clientFactory, typeMapper);
        this.accessor = new DomainValueAccessor();
        this.entityMapper = new MilvusEntityMapper(accessor);
        this.filterExpressionBuilder = new FilterExpressionBuilder(accessor);
    }

    @Override
    public <T> void ensureCollection(Class<T> domainType) {
        collectionManager.ensureCollection(metadataParser.parse(domainType));
    }

    @Override
    public <T> UpsertResult upsert(T entity) {
        if (entity == null) {
            return new UpsertResult(0);
        }
        return upsertBatch(Collections.singletonList(entity));
    }

    @Override
    public <T> UpsertResult upsertBatch(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return new UpsertResult(0);
        }
        Class<?> domainType = entities.iterator().next().getClass();
        VectorCollectionMetadata metadata = metadataParser.parse(domainType);
        collectionManager.ensureCollection(metadata);
        fillMissingEmbeddings(metadata, entities);

        List<JsonObject> rows = new ArrayList<>();
        for (T entity : entities) {
            rows.add(entityMapper.toRow(metadata, entity));
        }
        client().insert(InsertReq.builder()
                .collectionName(metadata.getCollectionName())
                .data(rows)
                .build());
        return new UpsertResult(rows.size());
    }

    @Override
    public <T> SearchResult<T> search(Class<T> domainType, String query, T filter, SearchOptions options) {
        if (query == null || query.trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID, "query 不能为空");
        }
        VectorCollectionMetadata metadata = metadataParser.parse(domainType);
        VectorFieldMetadata vectorField = resolveDefaultVectorField(metadata);
        EmbeddingFieldMetadata embeddingField = metadata.getEmbeddingFields().isEmpty()
                ? null
                : metadata.getEmbeddingFields().get(0);
        String providerName = resolveProviderName(embeddingField);
        EmbeddingProvider provider = embeddingProviderRegistry.require(providerName);
        EmbeddingOptions embeddingOptions = EmbeddingOptions.builder()
                .provider(providerName)
                .model(embeddingField == null ? "" : embeddingField.getModel())
                .build();
        List<Float> vector = provider.embed(Collections.singletonList(query), embeddingOptions).get(0);
        validateVectorDimension(vectorField, vector);
        return searchByVector(domainType, vector, filter, options);
    }

    @Override
    public <T> SearchResult<T> searchByVector(Class<T> domainType, List<Float> vector, T filter, SearchOptions options) {
        VectorCollectionMetadata metadata = metadataParser.parse(domainType);
        collectionManager.ensureCollection(metadata);
        VectorFieldMetadata vectorField = resolveDefaultVectorField(metadata);
        validateVectorDimension(vectorField, vector);

        SearchOptions actualOptions = options == null ? SearchOptions.topK(10) : options;
        String filterExpression = filterExpressionBuilder.build(metadata, filter, actualOptions.getFilterConditions());
        SearchReq.SearchReqBuilder builder = SearchReq.builder()
                .collectionName(metadata.getCollectionName())
                .data(Collections.singletonList(new FloatVec(vector)))
                .annsField(vectorField.getVectorName())
                .metricType(typeMapper.toMilvusMetricType(vectorField.getMetricType()))
                .topK(Math.max(1, actualOptions.getTopK()))
                .searchParams(resolveSearchParams(actualOptions))
                .outputFields(resolveOutputFields(metadata, actualOptions));
        if (filterExpression != null && !filterExpression.trim().isEmpty()) {
            builder.filter(filterExpression);
        }

        SearchResp response = client().search(builder.build());
        if (response.getSearchResults().isEmpty()) {
            return SearchResult.empty();
        }
        List<SearchHit<T>> hits = new ArrayList<>();
        for (SearchResp.SearchResult result : response.getSearchResults().get(0)) {
            if (actualOptions.getMinScore() != null && result.getScore() < actualOptions.getMinScore()) {
                continue;
            }
            Map<String, Object> values = new LinkedHashMap<>(result.getEntity());
            T entity = entityMapper.toEntity(metadata, values, result.getId());
            hits.add(new SearchHit<>(result.getScore(), entity));
        }
        return new SearchResult<>(hits);
    }

    @Override
    public <T> DeleteResult delete(Class<T> domainType, T filter) {
        VectorCollectionMetadata metadata = metadataParser.parse(domainType);
        collectionManager.ensureCollection(metadata);
        String filterExpression = filterExpressionBuilder.build(metadata, filter);
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "删除条件不能为空，避免误删整个 collection: " + metadata.getCollectionName());
        }
        client().delete(DeleteReq.builder()
                .collectionName(metadata.getCollectionName())
                .filter(filterExpression)
                .build());
        return new DeleteResult(-1);
    }

    @Override
    public <T, ID> DeleteResult deleteById(Class<T> domainType, ID id) {
        VectorCollectionMetadata metadata = metadataParser.parse(domainType);
        collectionManager.ensureCollection(metadata);
        client().delete(DeleteReq.builder()
                .collectionName(metadata.getCollectionName())
                .filter(filterExpressionBuilder.buildIdFilter(metadata, id))
                .build());
        return new DeleteResult(1);
    }

    @Override
    public <T> VectorDiagnosis diagnose(Class<T> domainType) {
        List<String> messages = new ArrayList<>();
        VectorCollectionMetadata metadata = metadataParser.parse(domainType);
        messages.add("domain 注解解析成功: " + domainType.getName());
        messages.add("collection: " + metadata.getCollectionName());
        messages.add("milvus uri: " + properties.getUri());
        messages.add("field count: " + metadata.getFields().size());
        messages.add("index count: " + metadata.getIndexes().size());
        for (EmbeddingFieldMetadata embeddingField : metadata.getEmbeddingFields()) {
            messages.add("embedding field: " + embeddingField.getTextField()
                    + " -> " + embeddingField.getVectorField()
                    + ", provider=" + resolveProviderName(embeddingField));
        }
        return new VectorDiagnosis(true, messages);
    }

    public EmbeddingProviderRegistry getEmbeddingProviderRegistry() {
        return embeddingProviderRegistry;
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    private <T> void fillMissingEmbeddings(VectorCollectionMetadata metadata, Collection<T> entities) {
        for (EmbeddingFieldMetadata embeddingField : metadata.getEmbeddingFields()) {
            VectorFieldMetadata textField = metadata.findFieldByJavaName(embeddingField.getTextField())
                    .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                            "找不到 embedding 文本字段: " + embeddingField.getTextField()));
            VectorFieldMetadata vectorField = metadata.findFieldByJavaName(embeddingField.getVectorField())
                    .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                            "找不到 embedding 向量字段: " + embeddingField.getVectorField()));
            String providerName = resolveProviderName(embeddingField);
            EmbeddingProvider provider = embeddingProviderRegistry.require(providerName);
            EmbeddingOptions options = EmbeddingOptions.builder()
                    .provider(providerName)
                    .model(embeddingField.getModel())
                    .build();

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
                List<Float> vector = provider.embed(Collections.singletonList(String.valueOf(text)), options).get(0);
                validateVectorDimension(vectorField, vector);
                accessor.set(entity, vectorField, vector);
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

    private VectorFieldMetadata resolveDefaultVectorField(VectorCollectionMetadata metadata) {
        for (VectorFieldMetadata field : metadata.getFields()) {
            if (field.getType() == VectorDataType.FLOAT_VECTOR) {
                return field;
            }
        }
        throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                "domain 缺少 FLOAT_VECTOR 字段: " + metadata.getDomainType().getName());
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

    private Map<String, Object> resolveSearchParams(SearchOptions options) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ef", Math.max(64, options.getTopK() * 2));
        params.putAll(options.getSearchParams());
        return params;
    }

    private List<String> resolveOutputFields(VectorCollectionMetadata metadata, SearchOptions options) {
        if (!options.getOutputFields().isEmpty()) {
            List<String> fields = new ArrayList<>();
            for (String fieldName : options.getOutputFields()) {
                fields.add(metadata.findFieldByName(fieldName)
                        .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                                "outputFields 指向的字段不存在: " + fieldName))
                        .getVectorName());
            }
            return fields;
        }
        List<String> fields = new ArrayList<>();
        for (VectorFieldMetadata field : metadata.getFields()) {
            if (field.isOutput()) {
                fields.add(field.getVectorName());
            }
        }
        return fields;
    }

    private MilvusClientV2 client() {
        return clientFactory.getClient();
    }
}
