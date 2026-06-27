package com.rongqi.vector.core.rerank;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RerankProvider 注册表。
 *
 * <p>用于保存内置和业务自定义的 rerank provider，并按名称查找。</p>
 */
public class RerankProviderRegistry {
    private final Map<String, RerankProvider> providers = new LinkedHashMap<>();

    /**
     * 注册 provider，同名时后注册的实现会覆盖旧实现。
     */
    public void register(RerankProvider provider) {
        if (provider == null || provider.name() == null || provider.name().trim().isEmpty()) {
            return;
        }
        providers.put(provider.name().trim(), provider);
    }

    /**
     * 判断指定 provider 是否存在。
     */
    public boolean contains(String name) {
        return name != null && providers.containsKey(name.trim());
    }

    /**
     * 获取指定 provider，不存在时抛出清晰异常。
     */
    public RerankProvider require(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("RerankProvider 名称不能为空");
        }
        RerankProvider provider = providers.get(name.trim());
        if (provider == null) {
            throw new IllegalArgumentException("RerankProvider 不存在: " + name);
        }
        return provider;
    }
}
