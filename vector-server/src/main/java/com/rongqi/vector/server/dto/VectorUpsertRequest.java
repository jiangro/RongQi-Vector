package com.rongqi.vector.server.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HTTP 写入请求。
 *
 * <p>domain 是业务 domain 的完整类名，items 中的字段名使用 Java 字段名，
 * 例如 tenantId、businessCode、content。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class VectorUpsertRequest {
    private String domain;
    private String collection;
    private List<Map<String, Object>> items = new ArrayList<>();
}
