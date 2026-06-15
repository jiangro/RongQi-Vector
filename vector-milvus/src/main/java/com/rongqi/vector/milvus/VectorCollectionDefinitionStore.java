package com.rongqi.vector.milvus;

import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import java.util.List;

/**
 * Collection schema 持久化存储接口。
 *
 * <p>注册表只依赖该接口，具体可以由本地文件、数据库或配置中心实现。</p>
 */
public interface VectorCollectionDefinitionStore {

    /**
     * 加载所有已持久化的 Collection schema。
     *
     * @return schema 定义列表
     */
    List<VectorCollectionDefinition> loadAll();

    /**
     * 保存或覆盖一个 Collection schema。
     *
     * @param definition schema 定义
     */
    void save(VectorCollectionDefinition definition);
}
