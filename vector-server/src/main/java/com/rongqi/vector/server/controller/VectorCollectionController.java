package com.rongqi.vector.server.controller;

import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import com.rongqi.vector.milvus.MilvusGenericTemplate;
import com.rongqi.vector.milvus.VectorCollectionDefinitionRegistry;
import com.rongqi.vector.server.dto.ApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP Schema Collection 管理接口。
 *
 * <p>不会 Java 的用户可以通过该接口定义 Collection、字段、索引和 Embedding 映射，
 * 效果等价于 Java 用户在 domain 上写 @VectorCollection、@VectorField、@VectorIndex。</p>
 */
@RestController
@RequestMapping("/api/vector/collections")
public class VectorCollectionController {
    private final MilvusGenericTemplate genericTemplate;
    private final VectorCollectionDefinitionRegistry definitionRegistry;

    public VectorCollectionController(MilvusGenericTemplate genericTemplate,
                                      VectorCollectionDefinitionRegistry definitionRegistry) {
        this.genericTemplate = genericTemplate;
        this.definitionRegistry = definitionRegistry;
    }

    /**
     * 创建并注册 Collection。
     */
    @PostMapping("/ensure")
    public ApiResponse<Map<String, Object>> ensure(@RequestBody VectorCollectionDefinition definition) {
        genericTemplate.ensureCollection(definition);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collection", definition.getCollection());
        data.put("fieldCount", definition.getFields().size());
        data.put("indexCount", definition.getIndexes().size());
        data.put("embeddingCount", definition.getEmbeddings().size());
        return ApiResponse.success(data);
    }

    /**
     * 查询当前服务已加载的 Collection schema 列表。
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list() {
        List<Map<String, Object>> collections = new ArrayList<>();
        for (VectorCollectionDefinition definition : definitionRegistry.listDefinitions()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("collection", definition.getCollection());
            item.put("database", definition.getDatabase());
            item.put("description", definition.getDescription());
            item.put("fieldCount", definition.getFields().size());
            item.put("indexCount", definition.getIndexes().size());
            item.put("embeddingCount", definition.getEmbeddings().size());
            collections.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", collections.size());
        data.put("collections", collections);
        return ApiResponse.success(data);
    }

    /**
     * 查询单个 Collection 的 schema 详情。
     */
    @GetMapping("/{collection}")
    public ApiResponse<VectorCollectionDefinition> detail(@PathVariable("collection") String collection) {
        VectorCollectionDefinition definition = definitionRegistry.find(collection);
        if (definition == null) {
            return ApiResponse.failed("VECTOR_COLLECTION_SCHEMA_NOT_FOUND",
                    "collection schema 未注册: " + collection);
        }
        return ApiResponse.success(definition);
    }
}
