package com.rongqi.vector.rerank.dashscope;

import com.google.gson.Gson;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.rerank.RerankDocument;
import com.rongqi.vector.core.rerank.RerankOptions;
import com.rongqi.vector.core.rerank.RerankResult;
import com.rongqi.vector.rerank.http.AbstractHttpRerankProvider;
import com.rongqi.vector.rerank.http.HttpRerankProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * DashScope RerankProvider。
 *
 * <p>默认使用 DashScope 文本重排风格请求体：model + input.query + input.documents。</p>
 */
public class DashScopeRerankProvider extends AbstractHttpRerankProvider {
    private static final Gson GSON = new Gson();

    public DashScopeRerankProvider(HttpRerankProperties properties) {
        super(properties);
    }

    @Override
    protected String buildRequestBody(String query, List<RerankDocument> documents, RerankOptions options) {
        List<String> texts = new ArrayList<>();
        if (documents != null) {
            for (RerankDocument document : documents) {
                texts.add(document == null ? "" : document.getText());
            }
        }
        return GSON.toJson(new DashScopeRequest(resolveModel(options), new DashScopeInput(query, texts)));
    }

    @Override
    protected List<RerankResult> parseResponse(String body) {
        DashScopeResponse response = GSON.fromJson(body, DashScopeResponse.class);
        if (response == null || response.output == null || response.output.results == null) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED, "DashScope Rerank 返回为空");
        }
        List<RerankResult> results = new ArrayList<>();
        for (DashScopeResult item : response.output.results) {
            double score = item.relevance_score == null ? item.score : item.relevance_score;
            results.add(new RerankResult(item.index, score));
        }
        return results;
    }

    private static class DashScopeRequest {
        private final String model;
        private final DashScopeInput input;

        private DashScopeRequest(String model, DashScopeInput input) {
            this.model = model;
            this.input = input;
        }
    }

    private static class DashScopeInput {
        private final String query;
        private final List<String> documents;

        private DashScopeInput(String query, List<String> documents) {
            this.query = query;
            this.documents = documents;
        }
    }

    private static class DashScopeResponse {
        private DashScopeOutput output;
    }

    private static class DashScopeOutput {
        private List<DashScopeResult> results;
    }

    private static class DashScopeResult {
        private int index;
        private Double relevance_score;
        private double score;
    }
}
