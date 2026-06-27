package com.rongqi.vector.core.rerank;

import java.util.List;

/**
 * Rerank 模型提供者接口。
 *
 * <p>RerankProvider 用于对 Milvus 第一阶段召回的候选结果重新打分，让更能回答 query 的内容排到前面。</p>
 */
public interface RerankProvider {

    /**
     * Provider 名称，用于配置和请求中选择具体 rerank 实现。
     */
    String name();

    /**
     * 对候选文档重新排序打分。
     *
     * @param query 用户搜索文本
     * @param documents 第一阶段召回的候选文档
     * @param options rerank 调用参数
     * @return rerank 分数结果
     */
    List<RerankResult> rerank(String query, List<RerankDocument> documents, RerankOptions options);
}
