package com.rongqi.vector.rerank.http;

import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.rerank.RerankDocument;
import com.rongqi.vector.core.rerank.RerankOptions;
import com.rongqi.vector.core.rerank.RerankProvider;
import com.rongqi.vector.core.rerank.RerankResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP RerankProvider 抽象基类。
 *
 * <p>该类负责通用 HTTP 调用、超时、重试和错误处理，具体请求体和响应解析由子类实现。</p>
 */
public abstract class AbstractHttpRerankProvider implements RerankProvider {
    private final HttpRerankProperties properties;

    protected AbstractHttpRerankProvider(HttpRerankProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return properties.getName();
    }

    @Override
    public List<RerankResult> rerank(String query, List<RerankDocument> documents, RerankOptions options) {
        if (properties.getUrl() == null || properties.getUrl().trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "HTTP RerankProvider 缺少 url: " + name());
        }
        VectorException lastException = null;
        int maxAttempts = properties.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return doRerank(query, documents, options);
            } catch (VectorException exception) {
                lastException = exception;
                if (!shouldRetry(exception, attempt, maxAttempts)) {
                    throw exception;
                }
                sleepBeforeRetry();
            }
        }
        throw lastException;
    }

    /**
     * 构建厂商特定请求体。
     */
    protected abstract String buildRequestBody(String query, List<RerankDocument> documents, RerankOptions options);

    /**
     * 解析厂商特定响应体。
     */
    protected abstract List<RerankResult> parseResponse(String body);

    protected String resolveModel(RerankOptions options) {
        if (options != null && options.getModel() != null && !options.getModel().trim().isEmpty()) {
            return options.getModel();
        }
        return properties.getModel();
    }

    private List<RerankResult> doRerank(String query, List<RerankDocument> documents, RerankOptions options) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(properties.getUrl()).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getTimeoutMillis());
            connection.setReadTimeout(properties.getTimeoutMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            }
            String body = buildRequestBody(query, documents, options);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            String responseBody = readResponse(connection, status);
            if (status < 200 || status >= 300) {
                throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                        "HTTP Rerank 调用失败，provider=" + name()
                                + ", model=" + resolveModel(options)
                                + ", status=" + status
                                + ", body=" + abbreviate(responseBody));
            }
            return parseResponse(responseBody);
        } catch (IOException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                    "HTTP Rerank 网络异常，provider=" + name()
                            + ", model=" + resolveModel(options)
                            + ": " + exception.getMessage(), exception);
        }
    }

    private String readResponse(HttpURLConnection connection, int status) throws IOException {
        if ((status < 200 || status >= 300) && connection.getErrorStream() == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private boolean shouldRetry(VectorException exception, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        Throwable cause = exception.getCause();
        if (cause instanceof IOException) {
            return true;
        }
        String message = exception.getMessage();
        return message != null && (message.contains("status=429")
                || message.contains("status=500")
                || message.contains("status=502")
                || message.contains("status=503")
                || message.contains("status=504"));
    }

    private void sleepBeforeRetry() {
        int interval = properties.getRetryIntervalMillis();
        if (interval <= 0) {
            return;
        }
        try {
            Thread.sleep(interval);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                    "HTTP Rerank 重试等待被中断: " + exception.getMessage(), exception);
        }
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
