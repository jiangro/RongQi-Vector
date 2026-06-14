package com.rongqi.vector.embedding;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EmbeddingProvider 注册表。
 *
 * <p>Spring Boot Starter 会把容器中的 provider 注册进来；普通 Java 项目也可以手动注册。</p>
 */
public class EmbeddingProviderRegistry {
    private final Map<String, EmbeddingProvider> providers = new LinkedHashMap<>();

    public EmbeddingProviderRegistry() {
    }

    public EmbeddingProviderRegistry(Collection<EmbeddingProvider> providers) {
        providers.forEach(this::register);
    }

    /**
     * 注册一个 provider。
     */
    public void register(EmbeddingProvider provider) {
        if (provider == null || provider.name() == null || provider.name().trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "EmbeddingProvider 名称不能为空");
        }
        providers.put(provider.name(), provider);
    }

    /**
     * 按名称查找 provider。
     */
    public EmbeddingProvider require(String name) {
        EmbeddingProvider provider = providers.get(name);
        if (provider == null) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_PROVIDER_NOT_FOUND,
                    "找不到 EmbeddingProvider: " + name);
        }
        return provider;
    }

    public boolean contains(String name) {
        return providers.containsKey(name);
    }
}
