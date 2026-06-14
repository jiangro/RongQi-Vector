package com.rongqi.vector.server.controller;

import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import com.rongqi.vector.milvus.MilvusGenericTemplate;
import com.rongqi.vector.server.dto.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
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

    public VectorCollectionController(MilvusGenericTemplate genericTemplate) {
        this.genericTemplate = genericTemplate;
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
}
