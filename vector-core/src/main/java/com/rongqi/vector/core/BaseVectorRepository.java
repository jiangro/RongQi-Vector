package com.rongqi.vector.core;

import java.util.Collection;

/**
 * Spring Repository 风格的基础接口。
 *
 * <p>Starter 后续会通过代理自动生成实现，让用户像使用 Mapper 一样使用向量搜索。</p>
 */
public interface BaseVectorRepository<T, ID> {
    UpsertResult save(T entity);

    UpsertResult saveBatch(Collection<T> entities);

    SearchResult<T> search(String query, T filter, SearchOptions options);

    DeleteResult delete(T filter);

    DeleteResult deleteById(ID id);
}
