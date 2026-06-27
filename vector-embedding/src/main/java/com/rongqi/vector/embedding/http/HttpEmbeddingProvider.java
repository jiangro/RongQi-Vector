package com.rongqi.vector.embedding.http;

import com.google.gson.Gson;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.embedding.EmbeddingOptions;
import com.rongqi.vector.embedding.EmbeddingProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用 HTTP EmbeddingProvider。
 *
 * <p>该 provider 默认发送 OpenAI 兼容格式：{"model":"...","input":[...]}。
 * 响应默认读取 OpenAI 兼容的 data[].embedding。公司内部模型服务只要兼容这个格式即可直接接入。</p>
 */
public class HttpEmbeddingProvider implements EmbeddingProvider {
    private static final Gson GSON = new Gson();
    private final HttpEmbeddingProperties properties;

    public HttpEmbeddingProvider(HttpEmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return properties.getName();
    }

    @Override
    public List<List<Float>> embed(List<String> texts, EmbeddingOptions options) {
        if (properties.getUrl() == null || properties.getUrl().trim().isEmpty()) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "HTTP EmbeddingProvider 缺少 url: " + name());
        }
        VectorException lastException = null;
        int maxAttempts = properties.getMaxRetries() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return doEmbed(texts, options);
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
     * 执行单次 HTTP Embedding 请求。
     */
    private List<List<Float>> doEmbed(List<String> texts, EmbeddingOptions options) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(resolveEndpoint()).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(properties.getTimeoutMillis());
            connection.setReadTimeout(properties.getTimeoutMillis());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            }

            String body = GSON.toJson(new EmbeddingRequest(resolveModel(options), texts));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            String responseBody = readResponse(connection, status);
            if (status < 200 || status >= 300) {
                throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                        "HTTP Embedding 调用失败，provider=" + name()
                                + ", model=" + resolveModel(options)
                                + ", status=" + status
                                + ", body=" + abbreviate(responseBody));
            }
            EmbeddingResponse response = GSON.fromJson(responseBody, EmbeddingResponse.class);
            if (response == null || response.data == null || response.data.isEmpty()) {
                throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                        "HTTP Embedding 返回为空: " + name());
            }
            List<List<Float>> vectors = new ArrayList<>();
            for (EmbeddingData item : response.data) {
                vectors.add(item.embedding);
            }
            return vectors;
        } catch (IOException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED,
                    "HTTP Embedding 网络异常，provider=" + name()
                            + ", model=" + resolveModel(options)
                            + ": " + exception.getMessage(), exception);
        }
    }

    @Override
    public int dimension(EmbeddingOptions options) {
        return properties.getDimension();
    }

    private String resolveModel(EmbeddingOptions options) {
        if (options != null && options.getModel() != null && !options.getModel().trim().isEmpty()) {
            return options.getModel();
        }
        return properties.getModel();
    }

    private String resolveEndpoint() {
        String url = properties.getUrl();
        if (url.endsWith("/embeddings")) {
            return url;
        }
        if (url.endsWith("/")) {
            return url + "embeddings";
        }
        return url + "/embeddings";
    }

    private String readResponse(HttpURLConnection connection, int status) throws IOException {
        if (status < 200 || status >= 300 && connection.getErrorStream() == null) {
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

    private String abbreviate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
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
                    "HTTP Embedding 重试等待被中断: " + exception.getMessage(), exception);
        }
    }

    private static class EmbeddingRequest {
        private final String model;
        private final List<String> input;

        private EmbeddingRequest(String model, List<String> input) {
            this.model = model;
            this.input = input;
        }
    }

    private static class EmbeddingResponse {
        private List<EmbeddingData> data;
    }

    private static class EmbeddingData {
        private List<Float> embedding;
    }
}
