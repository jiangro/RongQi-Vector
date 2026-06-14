package com.rongqi.vector.embedding;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import java.util.List;

/**
 * 占位 EmbeddingProvider。
 *
 * <p>当用户没有配置真实模型但又尝试自动生成向量时，抛出清晰错误，避免静默失败。</p>
 */
public class NoopEmbeddingProvider implements EmbeddingProvider {

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public List<List<Float>> embed(List<String> texts, EmbeddingOptions options) {
        throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_PROVIDER_NOT_FOUND,
                "未配置可用 EmbeddingProvider，请配置 rongqi.vector.embedding.default-provider 或直接传入向量字段");
    }

    @Override
    public int dimension(EmbeddingOptions options) {
        return -1;
    }
}
