package com.rongqi.vector.core;

import java.util.Collection;

/**
 * Spring Repository 风格的基础接口。
 *
 * <p>Starter 后续会通过代理自动生成实现，让用户像使用 Mapper 一样使用向量搜索。</p>
 */
public interface BaseVectorRepository<T, ID> {
    /**
     * 写入或更新单个实体。
     */
    UpsertResult save(T entity);

    /**
     * 批量写入或更新实体。
     */
    UpsertResult saveBatch(Collection<T> entities);

    /**
     * 根据文本和对象条件进行向量检索。
     */
    SearchResult<T> search(String query, T filter, SearchOptions options);

    /**
     * 根据对象条件删除数据。
     */
    DeleteResult delete(T filter);

    /**
     * 根据对象条件和复杂过滤条件删除数据。
     */
    DeleteResult delete(T filter, DeleteOptions options);

    /**
     * 根据主键删除数据。
     */
    DeleteResult deleteById(ID id);
}
