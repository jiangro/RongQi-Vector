package com.rongqi.vector.embedding;

import java.util.List;

/**
 * Embedding 模型统一接口。
 *
 * <p>RongQi Vector 通过该接口兼容 DashScope、OpenAI、Ollama、本地模型和用户自定义模型。</p>
 */
public interface EmbeddingProvider {

    /**
     * Provider 唯一名称，例如 dashscope、openai、local-bge。
     */
    String name();

    /**
     * 批量生成文本向量。
     */
    List<List<Float>> embed(List<String> texts, EmbeddingOptions options);

    /**
     * 当前模型返回的向量维度。
     */
    int dimension(EmbeddingOptions options);
}

