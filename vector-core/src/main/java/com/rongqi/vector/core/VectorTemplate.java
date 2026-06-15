package com.rongqi.vector.core;

import java.util.Collection;
import java.util.List;

/**
 * RongQi Vector 的主要调用入口。
 *
 * <p>业务方优先使用这个接口完成 collection 创建、写入、搜索和删除，
 * 不需要直接接触 Milvus SDK。</p>
 */
public interface VectorTemplate {

    /**
     * 根据 domain 注解创建或校验 collection。
     */
    <T> void ensureCollection(Class<T> domainType);

    /**
     * 写入单个 domain 对象。
     */
    <T> UpsertResult upsert(T entity);

    /**
     * 批量写入 domain 对象。
     */
    <T> UpsertResult upsertBatch(Collection<T> entities);

    /**
     * 根据文本 query 和对象条件进行向量检索。
     */
    <T> SearchResult<T> search(Class<T> domainType, String query, T filter, SearchOptions options);

    /**
     * 根据已有向量和对象条件进行向量检索。
     */
    <T> SearchResult<T> searchByVector(Class<T> domainType, List<Float> vector, T filter, SearchOptions options);

    /**
     * 根据对象条件删除数据。
     */
    <T> DeleteResult delete(Class<T> domainType, T filter);

    /**
     * 根据对象条件和复杂过滤条件删除数据。
     */
    <T> DeleteResult delete(Class<T> domainType, T filter, DeleteOptions options);

    /**
     * 根据主键删除数据。
     */
    <T, ID> DeleteResult deleteById(Class<T> domainType, ID id);

    /**
     * 诊断 domain、schema、索引和 embedding provider 是否配置正确。
     */
    <T> VectorDiagnosis diagnose(Class<T> domainType);
}
