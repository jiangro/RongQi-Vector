package com.rongqi.vector.milvus;

import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.metadata.VectorCollectionMetadata;
import com.rongqi.vector.core.metadata.VectorFieldMetadata;
import com.rongqi.vector.core.metadata.VectorIndexMetadata;
import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import com.rongqi.vector.core.schema.VectorFieldDefinition;
import com.rongqi.vector.core.schema.VectorIndexDefinition;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import java.util.ArrayList;
import java.util.List;

/**
 * Milvus Collection 创建、索引创建和加载管理。
 */
public class MilvusCollectionManager {
    private final MilvusClientFactory clientFactory;
    private final MilvusTypeMapper typeMapper;

    public MilvusCollectionManager(MilvusClientFactory clientFactory, MilvusTypeMapper typeMapper) {
        this.clientFactory = clientFactory;
        this.typeMapper = typeMapper;
    }

    /**
     * 根据 domain 元数据确保 collection 可用。
     *
     * <p>第一版采用保守策略：collection 已存在时不自动修改 schema，避免误改生产结构。</p>
     */
    public void ensureCollection(VectorCollectionMetadata metadata) {
        boolean exists = client().hasCollection(HasCollectionReq.builder()
                .collectionName(metadata.getCollectionName())
                .build());
        if (exists) {
            loadCollectionIfNeeded(metadata);
            return;
        }
        if (!metadata.isAutoCreate()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "Milvus collection 不存在且 autoCreate=false: " + metadata.getCollectionName());
        }

        CreateCollectionReq.CollectionSchema schema = client().createSchema();
        for (VectorFieldMetadata field : metadata.getFields()) {
            schema.addField(toAddFieldReq(field));
        }
        client().createCollection(CreateCollectionReq.builder()
                .collectionName(metadata.getCollectionName())
                .collectionSchema(schema)
                .build());

        if (metadata.isAutoCreateIndex() && !metadata.getIndexes().isEmpty()) {
            createIndexes(metadata);
        }
        loadCollectionIfNeeded(metadata);
    }

    /**
     * 根据 HTTP Schema 定义确保 collection 可用。
     */
    public void ensureCollection(VectorCollectionDefinition definition) {
        boolean exists = client().hasCollection(HasCollectionReq.builder()
                .collectionName(definition.getCollection())
                .build());
        if (exists) {
            loadCollectionIfNeeded(definition.getCollection());
            return;
        }
        if (!definition.isAutoCreate()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "Milvus collection 不存在且 autoCreate=false: " + definition.getCollection());
        }

        CreateCollectionReq.CollectionSchema schema = client().createSchema();
        for (VectorFieldDefinition field : definition.getFields()) {
            schema.addField(toAddFieldReq(field));
        }
        client().createCollection(CreateCollectionReq.builder()
                .collectionName(definition.getCollection())
                .collectionSchema(schema)
                .build());

        if (definition.isAutoCreateIndex() && !definition.getIndexes().isEmpty()) {
            createIndexes(definition);
        }
        loadCollectionIfNeeded(definition.getCollection());
    }

    /**
     * 判断 Milvus 中是否存在指定 Collection。
     *
     * @param collectionName Collection 名称
     * @return 存在时返回 true
     */
    public boolean hasCollection(String collectionName) {
        return client().hasCollection(HasCollectionReq.builder()
                .collectionName(collectionName)
                .build());
    }

    /**
     * 判断指定 Collection 是否已加载。
     *
     * @param collectionName Collection 名称
     * @return 已加载时返回 true
     */
    public boolean isLoaded(String collectionName) {
        return Boolean.TRUE.equals(client().getLoadState(GetLoadStateReq.builder()
                .collectionName(collectionName)
                .build()));
    }

    private AddFieldReq toAddFieldReq(VectorFieldMetadata field) {
        AddFieldReq.AddFieldReqBuilder builder = AddFieldReq.builder()
                .fieldName(field.getVectorName())
                .dataType(typeMapper.toMilvusDataType(field.getType()));

        if (field.isId()) {
            builder.isPrimaryKey(true).autoID(field.isAutoId());
        }
        if (field.getType() == VectorDataType.VARCHAR) {
            builder.maxLength(field.getMaxLength() > 0 ? field.getMaxLength() : 512);
        }
        if (field.getType() == VectorDataType.FLOAT_VECTOR
                || field.getType() == VectorDataType.BINARY_VECTOR
                || field.getType() == VectorDataType.SPARSE_FLOAT_VECTOR) {
            if (field.getDimension() <= 0 && field.getType() != VectorDataType.SPARSE_FLOAT_VECTOR) {
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "向量字段必须设置 dimension: " + field.getJavaName());
            }
            if (field.getDimension() > 0) {
                builder.dimension(field.getDimension());
            }
        }
        return builder.build();
    }

    private AddFieldReq toAddFieldReq(VectorFieldDefinition field) {
        AddFieldReq.AddFieldReqBuilder builder = AddFieldReq.builder()
                .fieldName(field.getName())
                .dataType(typeMapper.toMilvusDataType(field.getType()));

        if (field.isPrimaryKey()) {
            builder.isPrimaryKey(true).autoID(field.isAutoId());
        }
        if (field.getType() == VectorDataType.VARCHAR) {
            builder.maxLength(field.getMaxLength() > 0 ? field.getMaxLength() : 512);
        }
        if (field.getType() == VectorDataType.FLOAT_VECTOR
                || field.getType() == VectorDataType.BINARY_VECTOR
                || field.getType() == VectorDataType.SPARSE_FLOAT_VECTOR) {
            if (field.getDimension() <= 0 && field.getType() != VectorDataType.SPARSE_FLOAT_VECTOR) {
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "向量字段必须设置 dimension: " + field.getName());
            }
            if (field.getDimension() > 0) {
                builder.dimension(field.getDimension());
            }
        }
        return builder.build();
    }

    private void createIndexes(VectorCollectionMetadata metadata) {
        List<IndexParam> indexParams = new ArrayList<>();
        for (VectorIndexMetadata index : metadata.getIndexes()) {
            VectorFieldMetadata field = metadata.findFieldByJavaName(index.getJavaFieldName())
                    .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                            "@VectorIndex 指向的字段不存在: " + index.getJavaFieldName()));

            IndexParam.IndexParamBuilder builder = IndexParam.builder()
                    .fieldName(field.getVectorName())
                    .indexName(index.getIndexName())
                    .indexType(typeMapper.toMilvusIndexType(index.getIndexType()));

            if (field.getType() == VectorDataType.FLOAT_VECTOR
                    || field.getType() == VectorDataType.BINARY_VECTOR
                    || field.getType() == VectorDataType.SPARSE_FLOAT_VECTOR) {
                builder.metricType(typeMapper.toMilvusMetricType(index.getMetricType()));
            }
            if (!index.getParams().isEmpty()) {
                builder.extraParams(index.getParams());
            }
            indexParams.add(builder.build());
        }
        client().createIndex(CreateIndexReq.builder()
                .collectionName(metadata.getCollectionName())
                .indexParams(indexParams)
                .build());
    }

    private void createIndexes(VectorCollectionDefinition definition) {
        List<IndexParam> indexParams = new ArrayList<>();
        for (VectorIndexDefinition index : definition.getIndexes()) {
            VectorFieldDefinition field = definition.getFields().stream()
                    .filter(item -> item.getName().equals(index.getField()))
                    .findFirst()
                    .orElseThrow(() -> new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                            "索引指向的字段不存在: " + index.getField()));

            IndexParam.IndexParamBuilder builder = IndexParam.builder()
                    .fieldName(field.getName())
                    .indexName(hasText(index.getName()) ? index.getName() : "idx_" + field.getName())
                    .indexType(typeMapper.toMilvusIndexType(index.getType()));

            if (field.getType() == VectorDataType.FLOAT_VECTOR
                    || field.getType() == VectorDataType.BINARY_VECTOR
                    || field.getType() == VectorDataType.SPARSE_FLOAT_VECTOR) {
                builder.metricType(typeMapper.toMilvusMetricType(index.getMetricType()));
            }
            if (!index.getParams().isEmpty()) {
                builder.extraParams(index.getParams());
            }
            indexParams.add(builder.build());
        }
        client().createIndex(CreateIndexReq.builder()
                .collectionName(definition.getCollection())
                .indexParams(indexParams)
                .build());
    }

    private void loadCollectionIfNeeded(VectorCollectionMetadata metadata) {
        loadCollectionIfNeeded(metadata.getCollectionName());
    }

    private void loadCollectionIfNeeded(String collectionName) {
        Boolean loaded = client().getLoadState(GetLoadStateReq.builder()
                .collectionName(collectionName)
                .build());
        if (Boolean.TRUE.equals(loaded)) {
            return;
        }
        client().loadCollection(LoadCollectionReq.builder()
                .collectionName(collectionName)
                .sync(true)
                .build());
    }

    private MilvusClientV2 client() {
        return clientFactory.getClient();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
