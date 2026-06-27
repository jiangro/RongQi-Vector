package com.rongqi.vector.rerank.bge;

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
 * BGE RerankProvider。
 *
 * <p>默认兼容常见本地 BGE reranker HTTP 服务：model + query + documents。</p>
 */
public class BgeRerankProvider extends AbstractHttpRerankProvider {
    private static final Gson GSON = new Gson();

    public BgeRerankProvider(HttpRerankProperties properties) {
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
        return GSON.toJson(new BgeRequest(resolveModel(options), query, texts));
    }

    @Override
    protected List<RerankResult> parseResponse(String body) {
        BgeResponse response = GSON.fromJson(body, BgeResponse.class);
        if (response == null) {
            throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED, "BGE Rerank 返回为空");
        }
        if (response.results != null) {
            List<RerankResult> results = new ArrayList<>();
            for (BgeResult item : response.results) {
                results.add(new RerankResult(item.index, item.score));
            }
            return results;
        }
        if (response.scores != null) {
            List<RerankResult> results = new ArrayList<>();
            for (int index = 0; index < response.scores.size(); index++) {
                results.add(new RerankResult(index, response.scores.get(index)));
            }
            return results;
        }
        throw new VectorException(VectorErrorCode.VECTOR_EMBEDDING_FAILED, "BGE Rerank 返回缺少 results 或 scores");
    }

    private static class BgeRequest {
        private final String model;
        private final String query;
        private final List<String> documents;

        private BgeRequest(String model, String query, List<String> documents) {
            this.model = model;
            this.query = query;
            this.documents = documents;
        }
    }

    private static class BgeResponse {
        private List<BgeResult> results;
        private List<Double> scores;
    }

    private static class BgeResult {
        private int index;
        private double score;
    }
}
