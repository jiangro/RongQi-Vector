package com.rongqi.vector.milvus;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;

/**
 * MilvusClientV2 懒加载工厂。
 *
 * <p>Milvus client 创建成本较高，框架应复用同一个 client，避免每次请求都创建连接。</p>
 */
public class MilvusClientFactory implements AutoCloseable {
    private final MilvusClientProperties properties;
    private volatile MilvusClientV2 client;

    public MilvusClientFactory(MilvusClientProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取可复用的 Milvus client。
     */
    public MilvusClientV2 getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                            .uri(properties.getUri());
                    if (hasText(properties.getToken())) {
                        builder.token(properties.getToken());
                    }
                    if (hasText(properties.getDatabase())) {
                        builder.dbName(properties.getDatabase());
                    }
                    client = new MilvusClientV2(builder.build());
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        MilvusClientV2 current = client;
        client = null;
        if (current != null) {
            current.close();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

