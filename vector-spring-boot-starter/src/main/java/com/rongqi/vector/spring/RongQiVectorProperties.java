package com.rongqi.vector.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RongQi Vector Spring Boot 配置。
 *
 * <p>配置文件只放连接、模型和扫描参数，不放业务 collection schema。
 * collection schema 由业务 domain 注解决定。</p>
 */
@ConfigurationProperties(prefix = "rongqi.vector")
public class RongQiVectorProperties {
    private final Milvus milvus = new Milvus();
    private final Embedding embedding = new Embedding();
    private final HttpEmbedding httpEmbedding = new HttpEmbedding();
    private final Rerank rerank = new Rerank();
    private final Schema schema = new Schema();

    public Milvus getMilvus() {
        return milvus;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public HttpEmbedding getHttpEmbedding() {
        return httpEmbedding;
    }

    public Rerank getRerank() {
        return rerank;
    }

    public Schema getSchema() {
        return schema;
    }

    /**
     * Milvus 连接配置。
     */
    public static class Milvus {
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

    /**
     * Embedding 默认 provider 配置。
     */
    public static class Embedding {
        private String defaultProvider = "noop";
        private int batchSize = 32;

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public int getBatchSize() {
            return batchSize <= 0 ? 32 : batchSize;
        }

        /**
         * 设置批量 Embedding 的分批大小。
         *
         * <p>该值控制一次调用 EmbeddingProvider 时最多传入多少条文本，默认 32。</p>
         */
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    /**
     * 通用 HTTP Embedding Provider 配置。
     */
    public static class HttpEmbedding {
        private boolean enabled = false;
        private String name = "http";
        private String url;
        private String apiKey;
        private String model;
        private int dimension = 1024;
        private int timeoutMillis = 30000;
        private int maxRetries = 2;
        private int retryIntervalMillis = 500;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getMaxRetries() {
            return Math.max(0, maxRetries);
        }

        /**
         * 设置 HTTP Embedding 最大重试次数。
         *
         * <p>该值表示首次请求失败后最多再试几次，默认 2 次。</p>
         */
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryIntervalMillis() {
            return Math.max(0, retryIntervalMillis);
        }

        /**
         * 设置 HTTP Embedding 重试间隔毫秒数。
         */
        public void setRetryIntervalMillis(int retryIntervalMillis) {
            this.retryIntervalMillis = retryIntervalMillis;
        }
    }

    /**
     * Rerank 模型配置。
     *
     * <p>Rerank 用于对向量召回后的候选结果再次打分排序，通常能提升语义搜索最终命中质量。</p>
     */
    public static class Rerank {
        private boolean enabled = false;
        private String defaultProvider = "dashscope";
        private int timeoutMillis = 30000;
        private int maxRetries = 2;
        private int retryIntervalMillis = 500;
        private final HttpRerank dashscope = new HttpRerank("dashscope",
                "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank",
                "gte-rerank");
        private final HttpRerank bge = new HttpRerank("bge",
                "http://127.0.0.1:8000/rerank",
                "bge-reranker-large");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getMaxRetries() {
            return Math.max(0, maxRetries);
        }

        /**
         * 设置 HTTP Rerank 最大重试次数。
         *
         * <p>该值表示首次请求失败后最多再试几次，默认 2 次。</p>
         */
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryIntervalMillis() {
            return Math.max(0, retryIntervalMillis);
        }

        /**
         * 设置 HTTP Rerank 重试间隔毫秒数。
         */
        public void setRetryIntervalMillis(int retryIntervalMillis) {
            this.retryIntervalMillis = retryIntervalMillis;
        }

        public HttpRerank getDashscope() {
            return dashscope;
        }

        public HttpRerank getBge() {
            return bge;
        }
    }

    /**
     * 单个 HTTP Rerank Provider 配置。
     */
    public static class HttpRerank {
        private boolean enabled = false;
        private String name;
        private String url;
        private String apiKey;
        private String model;

        public HttpRerank() {
        }

        public HttpRerank(String name, String url, String model) {
            this.name = name;
            this.url = url;
            this.model = model;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    /**
     * HTTP Schema 持久化配置。
     */
    public static class Schema {
        private String type = "file";
        private String storageDir = "data/rongqi-vector/collections";
        private final Jdbc jdbc = new Jdbc();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }

        public Jdbc getJdbc() {
            return jdbc;
        }
    }

    /**
     * JDBC schema 持久化配置。
     */
    public static class Jdbc {
        private String tableName = "vector_collection_schema";
        private boolean initializeSchema = true;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public boolean isInitializeSchema() {
            return initializeSchema;
        }

        public void setInitializeSchema(boolean initializeSchema) {
            this.initializeSchema = initializeSchema;
        }
    }
}
