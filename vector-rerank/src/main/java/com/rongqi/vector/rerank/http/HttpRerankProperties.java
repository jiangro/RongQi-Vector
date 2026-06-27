package com.rongqi.vector.rerank.http;

/**
 * HTTP RerankProvider 配置。
 */
public class HttpRerankProperties {
    private String name;
    private String url;
    private String apiKey;
    private String model;
    private int timeoutMillis = 30000;
    private int maxRetries = 2;
    private int retryIntervalMillis = 500;

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
     * 设置最大重试次数。
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryIntervalMillis() {
        return Math.max(0, retryIntervalMillis);
    }

    /**
     * 设置每次重试前等待的毫秒数。
     */
    public void setRetryIntervalMillis(int retryIntervalMillis) {
        this.retryIntervalMillis = retryIntervalMillis;
    }
}
