package com.rongqi.vector.server.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HTTP 搜索二次排序请求。
 *
 * <p>调用方可以只传字段加权，也可以预留 profile / rerankProvider 给后续模型重排使用。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class VectorRankRequest {
    private String profile;
    private List<FieldBoostRequest> fieldBoosts = new ArrayList<>();
    private String rerankProvider;
    private String rerankModel;
    private String rerankTextField;

    /**
     * 字段加权请求项。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldBoostRequest {
        private String field;
        private double weight;

        public FieldBoostRequest(String field, double weight) {
            this.field = field;
            this.weight = weight;
        }
    }
}
