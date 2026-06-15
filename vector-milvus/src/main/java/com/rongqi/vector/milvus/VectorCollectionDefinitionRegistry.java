package com.rongqi.vector.milvus;

import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP Schema Collection 定义注册表。
 *
 * <p>纯 HTTP 用户先调用创建 Collection 接口，服务会把 schema 注册到这里。
 * 后续 upsert/search/delete 通过 collection 名称找到对应字段、索引和 embedding 定义。</p>
 *
 * <p>注意：Milvus 中存在 collection 不等于 RongQi Vector 知道它的字段含义。
 * 因此注册表支持持久化，服务重启后会自动加载已注册的 schema。</p>
 */
public class VectorCollectionDefinitionRegistry {
    private final Map<String, VectorCollectionDefinition> definitions = new ConcurrentHashMap<>();
    private final VectorCollectionDefinitionStore store;

    /**
     * 创建只保存在内存中的注册表。
     */
    public VectorCollectionDefinitionRegistry() {
        this((VectorCollectionDefinitionStore) null);
    }

    /**
     * 创建可持久化的注册表。
     *
     * @param storageDir schema 文件存储目录。为空时只使用内存注册表。
     */
    public VectorCollectionDefinitionRegistry(Path storageDir) {
        this(storageDir == null ? null : new FileVectorCollectionDefinitionStore(storageDir));
    }

    /**
     * 创建使用指定存储实现的注册表。
     *
     * @param store schema 存储实现。为空时只使用内存注册表
     */
    public VectorCollectionDefinitionRegistry(VectorCollectionDefinitionStore store) {
        this.store = store;
        loadFromStore();
    }

    /**
     * 注册或覆盖一个 Collection 定义。
     */
    public void register(VectorCollectionDefinition definition) {
        if (definition == null || definition.getCollection() == null || definition.getCollection().trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID, "collection 不能为空");
        }
        definitions.put(definition.getCollection(), definition);
        if (store != null) {
            store.save(definition);
        }
    }

    /**
     * 根据 Collection 名称获取定义。
     */
    public VectorCollectionDefinition require(String collection) {
        VectorCollectionDefinition definition = definitions.get(collection);
        if (definition == null) {
            throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                    "collection 未注册到 RongQi Vector 服务。Milvus 中存在 collection 不代表服务知道字段和 embedding 映射；"
                            + "请先调用 /api/vector/collections/ensure，或确认 schema 持久化目录已正确加载: " + collection);
        }
        return definition;
    }

    private void loadFromStore() {
        if (store == null) {
            return;
        }
        for (VectorCollectionDefinition definition : store.loadAll()) {
            if (definition != null && definition.getCollection() != null && !definition.getCollection().trim().isEmpty()) {
                definitions.put(definition.getCollection(), definition);
            }
        }
    }
}
