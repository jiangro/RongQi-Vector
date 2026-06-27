package com.rongqi.vector.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rongqi.vector.core.DeleteOptions;
import com.rongqi.vector.core.DeleteResult;
import com.rongqi.vector.core.SearchHit;
import com.rongqi.vector.core.SearchOptions;
import com.rongqi.vector.core.SearchResult;
import com.rongqi.vector.core.UpsertResult;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.VectorTemplate;
import com.rongqi.vector.core.rank.RankOptions;
import com.rongqi.vector.milvus.MilvusGenericTemplate;
import com.rongqi.vector.server.dto.ApiResponse;
import com.rongqi.vector.server.dto.VectorDeleteRequest;
import com.rongqi.vector.server.dto.VectorRankRequest;
import com.rongqi.vector.server.dto.VectorSearchHitResponse;
import com.rongqi.vector.server.dto.VectorSearchRequest;
import com.rongqi.vector.server.dto.VectorSearchResponse;
import com.rongqi.vector.server.dto.VectorUpsertRequest;
import com.rongqi.vector.server.dto.VectorWriteResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用向量数据 HTTP 接口。
 *
 * <p>HTTP 调用方通过 domain 完整类名指定业务模型，字段名使用 Java domain 字段名。
 * 服务端会把 JSON 转成对应 domain 对象，再调用 VectorTemplate。</p>
 */
@RestController
@RequestMapping("/api/vector")
public class VectorDocumentController {
    private final VectorTemplate vectorTemplate;
    private final MilvusGenericTemplate genericTemplate;
    private final ObjectMapper objectMapper;

    public VectorDocumentController(VectorTemplate vectorTemplate,
                                    MilvusGenericTemplate genericTemplate,
                                    ObjectMapper objectMapper) {
        this.vectorTemplate = vectorTemplate;
        this.genericTemplate = genericTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 批量写入向量数据。
     */
    @PostMapping("/documents/upsert")
    public ApiResponse<VectorWriteResponse> upsert(@RequestBody VectorUpsertRequest request) {
        validateTarget(request.getDomain(), request.getCollection());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID, "items 不能为空");
        }
        if (hasText(request.getCollection())) {
            UpsertResult result = genericTemplate.upsert(request.getCollection(), request.getItems());
            return ApiResponse.success(new VectorWriteResponse(result.getCount()));
        }
        Class<?> domainType = resolveDomainType(request.getDomain());
        List<Object> entities = new ArrayList<>();
        for (Map<String, Object> item : request.getItems()) {
            entities.add(toDomain(domainType, item));
        }
        UpsertResult result = vectorTemplate.upsertBatch(entities);
        return ApiResponse.success(new VectorWriteResponse(result.getCount()));
    }

    /**
     * 根据 query 或已有 vector 检索相似数据。
     */
    @PostMapping("/search")
    public ApiResponse<VectorSearchResponse> search(@RequestBody VectorSearchRequest request) {
        validateTarget(request.getDomain(), request.getCollection());
        validateQueryOrVector(request);
        if (hasText(request.getCollection())) {
            return searchByCollection(request);
        }
        Class<?> domainType = resolveDomainType(request.getDomain());
        Object filter = request.getFilterObject() == null || request.getFilterObject().isEmpty()
                ? null
                : toDomain(domainType, request.getFilterObject());
        SearchOptions options = buildSearchOptions(request);
        SearchResult<?> result;
        if (request.getVector() != null && !request.getVector().isEmpty()) {
            result = searchByVector(domainType, request.getVector(), filter, options);
        } else {
            result = search(domainType, request.getQuery(), filter, options);
        }
        List<VectorSearchHitResponse> hits = new ArrayList<>();
        for (SearchHit<?> hit : result.getHits()) {
            hits.add(new VectorSearchHitResponse(hit.getScore(), hit.getRankScore(), hit.getEntity()));
        }
        return ApiResponse.success(new VectorSearchResponse(hits));
    }

    /**
     * 按主键或对象条件删除数据。
     */
    @PostMapping("/documents/delete")
    public ApiResponse<VectorWriteResponse> delete(@RequestBody VectorDeleteRequest request) {
        validateTarget(request.getDomain(), request.getCollection());
        validateDeleteCondition(request);
        if (hasText(request.getCollection())) {
            DeleteResult result = genericTemplate.delete(
                    request.getCollection(),
                    request.getIds(),
                    request.getFilterObject(),
                    request.getFilters());
            return ApiResponse.success(new VectorWriteResponse(result.getCount()));
        }
        Class<?> domainType = resolveDomainType(request.getDomain());
        int count = 0;
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            for (Object id : request.getIds()) {
                DeleteResult result = deleteById(domainType, id);
                count += result.getCount();
            }
            return ApiResponse.success(new VectorWriteResponse(count));
        }
        Object filter = request.getFilterObject() == null || request.getFilterObject().isEmpty()
                ? null
                : toDomain(domainType, request.getFilterObject());
        DeleteResult result = delete(domainType, filter, buildDeleteOptions(request));
        return ApiResponse.success(new VectorWriteResponse(result.getCount()));
    }

    private ApiResponse<VectorSearchResponse> searchByCollection(VectorSearchRequest request) {
        SearchOptions options = buildSearchOptions(request);
        SearchResult<Map<String, Object>> result;
        if (request.getVector() != null && !request.getVector().isEmpty()) {
            result = genericTemplate.searchByVector(
                    request.getCollection(),
                    request.getVector(),
                    request.getFilterObject(),
                    options);
        } else {
            result = genericTemplate.search(
                    request.getCollection(),
                    request.getQuery(),
                    request.getFilterObject(),
                    options);
        }
        List<VectorSearchHitResponse> hits = new ArrayList<>();
        for (SearchHit<Map<String, Object>> hit : result.getHits()) {
            hits.add(new VectorSearchHitResponse(hit.getScore(), hit.getRankScore(), hit.getEntity()));
        }
        return ApiResponse.success(new VectorSearchResponse(hits));
    }

    @SuppressWarnings("unchecked")
    private SearchResult<Object> search(Class<?> domainType, String query, Object filter, SearchOptions options) {
        Class<Object> typedDomain = (Class<Object>) domainType;
        return vectorTemplate.search(typedDomain, query, typedDomain.cast(filter), options);
    }

    @SuppressWarnings("unchecked")
    private SearchResult<Object> searchByVector(Class<?> domainType, List<Float> vector, Object filter,
                                                SearchOptions options) {
        Class<Object> typedDomain = (Class<Object>) domainType;
        return vectorTemplate.searchByVector(typedDomain, vector, typedDomain.cast(filter), options);
    }

    @SuppressWarnings("unchecked")
    private DeleteResult delete(Class<?> domainType, Object filter) {
        Class<Object> typedDomain = (Class<Object>) domainType;
        return vectorTemplate.delete(typedDomain, typedDomain.cast(filter));
    }

    @SuppressWarnings("unchecked")
    private DeleteResult delete(Class<?> domainType, Object filter, DeleteOptions options) {
        Class<Object> typedDomain = (Class<Object>) domainType;
        return vectorTemplate.delete(typedDomain, typedDomain.cast(filter), options);
    }

    @SuppressWarnings("unchecked")
    private DeleteResult deleteById(Class<?> domainType, Object id) {
        Class<Object> typedDomain = (Class<Object>) domainType;
        return vectorTemplate.deleteById(typedDomain, id);
    }

    private SearchOptions buildSearchOptions(VectorSearchRequest request) {
        SearchOptions.Builder builder = SearchOptions.builder()
                .topK(request.getTopK() == null ? 10 : request.getTopK())
                .minScore(request.getMinScore());
        if (request.getCandidateTopK() != null) {
            builder.candidateTopK(request.getCandidateTopK());
        }
        if (request.getOutputFields() != null) {
            for (String outputField : request.getOutputFields()) {
                builder.outputFields(outputField);
            }
        }
        if (request.getFilters() != null) {
            request.getFilters().stream()
                    .filter(filter -> filter != null)
                    .forEach(filter -> builder.filter(filter.getField(), filter.getOperator(), filter.getValue()));
        }
        if (request.getRank() != null) {
            builder.rank(toRankOptions(request.getRank()));
        }
        return builder.build();
    }

    private RankOptions toRankOptions(VectorRankRequest request) {
        RankOptions.Builder builder = RankOptions.builder()
                .profile(request.getProfile())
                .rerankProvider(request.getRerankProvider())
                .rerankModel(request.getRerankModel())
                .rerankTextField(request.getRerankTextField());
        if (request.getFieldBoosts() != null) {
            for (VectorRankRequest.FieldBoostRequest fieldBoost : request.getFieldBoosts()) {
                if (fieldBoost != null) {
                    builder.fieldBoost(fieldBoost.getField(), fieldBoost.getWeight());
                }
            }
        }
        return builder.build();
    }

    private DeleteOptions buildDeleteOptions(VectorDeleteRequest request) {
        DeleteOptions.Builder builder = DeleteOptions.builder();
        if (request.getFilters() != null) {
            request.getFilters().stream()
                    .filter(filter -> filter != null)
                    .forEach(filter -> builder.filter(filter.getField(), filter.getOperator(), filter.getValue()));
        }
        return builder.build();
    }

    /**
     * 校验 domain 和 collection 只能选择一种调用模式。
     */
    private void validateTarget(String domain, String collection) {
        boolean hasDomain = hasText(domain);
        boolean hasCollection = hasText(collection);
        if (hasDomain == hasCollection) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "domain 和 collection 必须二选一，不能都为空或同时传入");
        }
    }

    /**
     * 校验搜索请求必须且只能提供 query 或 vector。
     */
    private void validateQueryOrVector(VectorSearchRequest request) {
        boolean hasQuery = hasText(request.getQuery());
        boolean hasVector = request.getVector() != null && !request.getVector().isEmpty();
        if (hasQuery == hasVector) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "query 和 vector 必须二选一，不能都为空或同时传入");
        }
    }

    /**
     * 校验删除请求至少提供一种删除条件，避免误删整个 Collection。
     */
    private void validateDeleteCondition(VectorDeleteRequest request) {
        boolean hasIds = request.getIds() != null && !request.getIds().isEmpty();
        boolean hasFilterObject = request.getFilterObject() != null && !request.getFilterObject().isEmpty();
        boolean hasFilters = request.getFilters() != null && !request.getFilters().isEmpty();
        if (!hasIds && !hasFilterObject && !hasFilters) {
            throw new VectorException(VectorErrorCode.VECTOR_FILTER_INVALID,
                    "删除请求必须提供 ids、filterObject 或 filters");
        }
    }

    private Class<?> resolveDomainType(String domainClassName) {
        if (domainClassName == null || domainClassName.trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID, "domain 不能为空");
        }
        try {
            return Class.forName(domainClassName);
        } catch (ClassNotFoundException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_DOMAIN_INVALID,
                    "找不到 domain 类，请确认该类已经在服务 classpath 中: " + domainClassName, exception);
        }
    }

    private Object toDomain(Class<?> domainType, Map<String, Object> values) {
        return objectMapper.convertValue(values, domainType);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
