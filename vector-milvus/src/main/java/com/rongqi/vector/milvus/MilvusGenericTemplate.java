package com.rongqi.vector.milvus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.core.DeleteResult;
import com.rongqi.vector.core.FilterCondition;
import com.rongqi.vector.core.FilterOperator;
import com.rongqi.vector.core.SearchHit;
import com.rongqi.vector.core.SearchOptions;
import com.rongqi.vector.core.SearchResult;
import com.rongqi.vector.core.UpsertResult;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import com.rongqi.vector.core.schema.VectorEmbeddingDefinition;
import com.rongqi.vector.core.schema.VectorFieldDefinition;
import com.rongqi.vector.embedding.EmbeddingOptions;
import com.rongqi.vector.embedding.EmbeddingProvider;
import com.rongqi.vector.embedding.EmbeddingProviderRegistry;
import io.milvus.v2.client.MilvusClientV2;
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
 * 不依赖 Java domain 的通用 Milvus 操作模板。
 *
 * <p>该模板服务纯 HTTP 调用场景，字段名直接使用接口定义的 schema 字段名。</p>
 */
public class MilvusGenericTemplate {
    private final String defaultEmbeddingProvider;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final MilvusClientFactory clientFactory;
    private final MilvusCollectionManager collectionManager;
    private final VectorCollectionDefinitionRegistry definitionRegistry;
    private final MilvusTypeMapper typeMapper;
    private final GenericEmbeddingBatchFiller embeddingBatchFiller;

    public MilvusGenericTemplate(MilvusClientProperties properties,
                                 EmbeddingProviderRegistry embeddingProviderRegistry,
                                 String defaultEmbeddingProvider,
                                 VectorCollectionDefinitionRegistry definitionRegistry) {
        this(properties, embeddingProviderRegistry, defaultEmbeddingProvider, definitionRegistry, 32);
    }

    /**
     * 创建 HTTP collection 模式的通用模板。
     *
     * @param properties Milvus 连接配置
     * @param embeddingProviderRegistry EmbeddingProvider 注册表
     * @param defaultEmbeddingProvider 默认 EmbeddingProvider 名称
     * @param definitionRegistry HTTP collection schema 注册表
     * @param embeddingBatchSize 每次批量生成 embedding 的文本数量
     */
    public MilvusGenericTemplate(MilvusClientProperties properties,
                                 EmbeddingProviderRegistry embeddingProviderRegistry,
                                 String defaultEmbeddingProvider,
                                 VectorCollectionDefinitionRegistry definitionRegistry,
                                 int embeddingBatchSize) {
        this.defaultEmbeddingProvider = defaultEmbeddingProvider == null || defaultEmbeddingProvider.trim().isEmpty()
                ? "noop"
                : defaultEmbeddingProvider;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.clientFactory = new MilvusClientFactory(properties);
        this.typeMapper = new MilvusTypeMapper();
        this.collectionManager = new MilvusCollectionManager(clientFactory, typeMapper);
        this.definitionRegistry = definitionRegistry;
        this.embeddingBatchFiller = new GenericEmbeddingBatchFiller(
                embeddingProviderRegistry,
                this.defaultEmbeddingProvider,
                embeddingBatchSize);
    }

    /**
     * 创建并注册 Collection。
     */
    public void ensureCollection(VectorCollectionDefinition definition) {
        validateDefinition(definition);
        collectionManager.ensureCollection(definition);
        definitionRegistry.register(definition);
    }

    /**
     * 按 collection 写入 Map 数据。
     */
    public UpsertResult upsert(String collection, Collection<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return new UpsertResult(0);
        }
        VectorCollectionDefinition definition = definitionRegistry.require(collection);
        collectionManager.ensureCollection(definition);
        embeddingBatchFiller.fillMissingEmbeddings(definition, items);
        List<JsonObject> rows = new ArrayList<>();
        for (Map<String, Object> item : items) {
            rows.add(toRow(definition, item));
        }
        client().insert(InsertReq.builder()
                .collectionName(definition.getCollection())
                .data(rows)
                .build());
        return new UpsertResult(rows.size());
    }

    /**
     * 按文本 query 搜索。
     */
    public SearchResult<Map<String, Object>> search(String collection, String query,
                                                    Map<String, Object> filterObject,
                                                    SearchOptions options) {
        VectorCollectionDefinition definition = definitionRegistry.require(collection);
        VectorFieldDefinition vectorField = resolveDefaultVectorField(definition);
        VectorEmbeddingDefinition embedding = definition.getEmbeddings().isEmpty()
                ? null
                : definition.getEmbeddings().get(0);
        String providerName = resolveProviderName(embedding);
        EmbeddingProvider provider = embeddingProviderRegistry.require(providerName);
        List<Float> vector = provider.embed(Collections.singletonList(query),
                EmbeddingOptions.builder()
                        .provider(providerName)
                        .model(embedding == null ? "" : embedding.getModel())
                        .build()).get(0);
        validateVectorDimension(vectorField, vector);
        return searchByVector(collection, vector, filterObject, options);
    }

    /**
     * 按已有向量搜索。
     */
    public SearchResult<Map<String, Object>> searchByVector(String collection, List<Float> vector,
                                                            Map<String, Object> filterObject,
                                                            SearchOptions options) {
        VectorCollectionDefinition definition = definitionRegistry.require(collection);
        collectionManager.ensureCollection(definition);
        VectorFieldDefinition vectorField = resolveDefaultVectorField(definition);
        validateVectorDimension(vectorField, vector);
        SearchOptions actualOptions = options == null ? SearchOptions.topK(10) : options;

        SearchReq.SearchReqBuilder builder = SearchReq.builder()
                .collectionName(definition.getCollection())
                .data(Collections.singletonList(new FloatVec(vector)))
                .annsField(vectorField.getName())
                .metricType(typeMapper.toMilvusMetricType(vectorField.getMetricType()))
                .topK(Math.max(1, actualOptions.getTopK()))
                .searchParams(resolveSearchParams(actualOptions))
                .outputFields(resolveOutputFields(definition, actualOptions));
        String filter = buildFilter(definition, filterObject, actualOptions.getFilterConditions());
        if (filter != null && !filter.trim().isEmpty()) {
            builder.filter(filter);
        }
        SearchResp response = client().search(builder.build());
        if (response.getSearchResults().isEmpty()) {
            return SearchResult.empty();
        }
        List<SearchHit<Map<String, Object>>> hits = new ArrayList<>();
        for (SearchResp.SearchResult result : response.getSearchResults().get(0)) {
            if (actualOptions.getMinScore() != null && result.getScore() < actualOptions.getMinScore()) {
                continue;
            }
            Map<String, Object> entity = new LinkedHashMap<>(result.getEntity());
            VectorFieldDefinition idField = resolvePrimaryKey(definition);
            entity.putIfAbsent(idField.getName(), result.getId());
            hits.add(new SearchHit<>(result.getScore(), entity));
        }
        return new SearchResult<>(hits);
    }

    /**
     * 按主键或对象条件删除。
     */
    public DeleteResult delete(String collection, List<Object> ids, Map<String, Object> filterObject) {
        return delete(collection, ids, filterObject, Collections.emptyList());
    }

    /**
     * 按主键、对象条件或复杂过滤条件删除。
     */
    public DeleteResult delete(String collection, List<Object> ids, Map<String, Object> filterObject,
                               List<FilterCondition> filterConditions) {
        VectorCollectionDefinition definition = definitionRegistry.require(collection);
        collectionManager.ensureCollection(definition);
        int count = 0;
        if (ids != null && !ids.isEmpty()) {
            VectorFieldDefinition idField = resolvePrimaryKey(definition);
            for (Object id : ids) {
                client().delete(DeleteReq.builder()
                        .collectionName(definition.getCollection())
                        .filter(idField.getName() + " == " + formatValue(id))
                        .build());
                count++;
            }
            return new DeleteResult(count);
        }
        String filter = buildFilter(definition, filterObject, filterConditions);
        if (filter == null || filter.trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID, "删除条件不能为空，避免误删 collection: " + collection);
        }
        client().delete(DeleteReq.builder()
                .collectionName(definition.getCollection())
                .filter(filter)
                .build());
        return new DeleteResult(-1);
    }

    private void validateDefinition(VectorCollectionDefinition definition) {
        if (definition == null || definition.getCollection() == null || definition.getCollection().trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID, "collection 不能为空");
        }
        long primaryCount = definition.getFields().stream().filter(VectorFieldDefinition::isPrimaryKey).count();
        if (primaryCount != 1) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID, "必须且只能定义一个 primaryKey 字段: " + definition.getCollection());
        }
    }

    @SuppressWarnings("unchecked")
    private JsonObject toRow(VectorCollectionDefinition definition, Map<String, Object> item) {
        JsonObject row = new JsonObject();
        for (VectorFieldDefinition field : definition.getFields()) {
            Object value = item.get(field.getName());
            if (value == null && field.isAutoId()) {
                continue;
            }
            if (value == null) {
                value = defaultValue(field);
            }
            switch (field.getType()) {
                case BOOL:
                    row.addProperty(field.getName(), (Boolean) value);
                    break;
                case INT8:
                case INT16:
                case INT32:
                case INT64:
                case FLOAT:
                case DOUBLE:
                    row.addProperty(field.getName(), (Number) value);
                    break;
                case VARCHAR:
                    row.addProperty(field.getName(), truncate(String.valueOf(value), field.getMaxLength()));
                    break;
                case FLOAT_VECTOR:
                    row.add(field.getName(), toFloatArray((List<Float>) value));
                    break;
                default:
                    throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                            "暂不支持 HTTP 写入字段类型: " + field.getType() + ", field=" + field.getName());
            }
        }
        return row;
    }

    private String buildFilter(VectorCollectionDefinition definition, Map<String, Object> filterObject) {
        return buildFilter(definition, filterObject, Collections.emptyList());
    }

    private String buildFilter(VectorCollectionDefinition definition, Map<String, Object> filterObject,
                               List<FilterCondition> filterConditions) {
        List<String> conditions = new ArrayList<>();
        if (filterObject == null || filterObject.isEmpty()) {
            // 没有旧版 filterObject 条件。
        } else {
            // 兼容旧用法：filterObject 中的字段按等值条件处理，集合值按 in 条件处理。
            for (Map.Entry<String, Object> entry : filterObject.entrySet()) {
                VectorFieldDefinition field = requireField(definition, entry.getKey());
                validateFilterable(field);
                if (entry.getValue() instanceof Collection) {
                    conditions.add(toListCondition(field.getName(), "in", entry.getValue()));
                } else {
                    conditions.add(toCondition(field.getName(), FilterOperator.EQ, entry.getValue()));
                }
            }
        }
        if (filterConditions != null) {
            // 新用法：SearchOptions 中的显式条件支持范围、列表和模糊匹配。
            for (FilterCondition condition : filterConditions) {
                if (condition == null || condition.getField() == null || condition.getField().trim().isEmpty()) {
                    continue;
                }
                VectorFieldDefinition field = requireField(definition, condition.getField());
                validateFilterable(field);
                conditions.add(toCondition(field.getName(), condition.getOperator(), condition.getValue()));
            }
        }
        return String.join(" and ", conditions);
    }

    private void validateFilterable(VectorFieldDefinition field) {
        if (!field.isPrimaryKey() && !field.isFilterable()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "字段不允许作为过滤条件: " + field.getName());
        }
    }

    private String toCondition(String fieldName, FilterOperator operator, Object value) {
        if (value == null) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "过滤条件的值不能为空: " + fieldName);
        }
        FilterOperator actualOperator = operator == null ? FilterOperator.EQ : operator;
        switch (actualOperator) {
            case EQ:
                if (value instanceof Collection) {
                    return toListCondition(fieldName, "in", value);
                }
                return fieldName + " == " + formatValue(value);
            case NE:
                return fieldName + " != " + formatValue(value);
            case GT:
                return fieldName + " > " + formatValue(value);
            case GTE:
                return fieldName + " >= " + formatValue(value);
            case LT:
                return fieldName + " < " + formatValue(value);
            case LTE:
                return fieldName + " <= " + formatValue(value);
            case IN:
                return toListCondition(fieldName, "in", value);
            case NOT_IN:
                return toListCondition(fieldName, "not in", value);
            case LIKE:
                if (!(value instanceof CharSequence)) {
                    throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                            "like 过滤条件的值必须是字符串: " + fieldName);
                }
                return fieldName + " like " + formatValue(value);
            default:
                throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                        "不支持的过滤操作符: " + actualOperator);
        }
    }

    private String toListCondition(String fieldName, String operator, Object value) {
        if (!(value instanceof Collection)) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    operator + " 过滤条件的值必须是集合: " + fieldName);
        }
        Collection<?> values = (Collection<?>) value;
        if (values.isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    operator + " 过滤条件的值不能为空: " + fieldName);
        }
        List<String> formattedValues = new ArrayList<>();
        for (Object item : values) {
            formattedValues.add(formatValue(item));
        }
        return fieldName + " " + operator + " [" + String.join(",", formattedValues) + "]";
    }

    private VectorFieldDefinition requireField(VectorCollectionDefinition definition, String fieldName) {
        return definition.getFields().stream()
                .filter(field -> field.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "字段不存在: " + fieldName + ", collection=" + definition.getCollection()));
    }

    private VectorFieldDefinition resolvePrimaryKey(VectorCollectionDefinition definition) {
        return definition.getFields().stream()
                .filter(VectorFieldDefinition::isPrimaryKey)
                .findFirst()
                .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "collection 缺少 primaryKey 字段: " + definition.getCollection()));
    }

    private VectorFieldDefinition resolveDefaultVectorField(VectorCollectionDefinition definition) {
        return definition.getFields().stream()
                .filter(field -> field.getType() == VectorDataType.FLOAT_VECTOR)
                .findFirst()
                .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "collection 缺少 FLOAT_VECTOR 字段: " + definition.getCollection()));
    }

    private String resolveProviderName(VectorEmbeddingDefinition embedding) {
        if (embedding != null && embedding.getProvider() != null && !embedding.getProvider().trim().isEmpty()) {
            return embedding.getProvider();
        }
        return defaultEmbeddingProvider;
    }

    private List<String> resolveOutputFields(VectorCollectionDefinition definition, SearchOptions options) {
        if (!options.getOutputFields().isEmpty()) {
            return options.getOutputFields();
        }
        List<String> fields = new ArrayList<>();
        for (VectorFieldDefinition field : definition.getFields()) {
            if (field.isOutput()) {
                fields.add(field.getName());
            }
        }
        return fields;
    }

    private Map<String, Object> resolveSearchParams(SearchOptions options) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ef", Math.max(64, options.getTopK() * 2));
        params.putAll(options.getSearchParams());
        return params;
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

    private JsonArray toFloatArray(List<Float> vector) {
        JsonArray array = new JsonArray();
        for (Float value : vector) {
            array.add(value);
        }
        return array;
    }

    private Object defaultValue(VectorFieldDefinition field) {
        if (field.isPrimaryKey()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "主键字段不能为空: " + field.getName());
        }
        switch (field.getType()) {
            case BOOL:
                return false;
            case INT8:
            case INT16:
            case INT32:
            case INT64:
                return 0L;
            case FLOAT:
            case DOUBLE:
                return 0D;
            case VARCHAR:
                return "";
            case FLOAT_VECTOR:
            case BINARY_VECTOR:
            case SPARSE_FLOAT_VECTOR:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "向量字段不能为空，请传入 " + field.getName() + " 或配置 embeddings 自动生成");
            default:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "字段缺失且暂不支持默认值: " + field.getName() + ", type=" + field.getType());
        }
    }

    private String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String formatValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private MilvusClientV2 client() {
        return clientFactory.getClient();
    }
}
