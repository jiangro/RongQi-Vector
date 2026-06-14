package com.rongqi.vector.milvus;

/**
 * Milvus 连接配置。
 *
 * <p>该类不依赖 Spring，普通 Java 项目也可以手动构造。</p>
 */
public class MilvusClientProperties {
    private String uri = "http://127.0.0.1:19530";
    private String token;
    private String database = "default";

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}

