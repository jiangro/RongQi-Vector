package com.rongqi.vector.server.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * HTTP 写入请求。
 *
 * <p>domain 是业务 domain 的完整类名，items 中的字段名使用 Java 字段名，
 * 例如 tenantId、businessCode、content。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class VectorUpsertRequest {
    @ToString.Include
    private String domain;
    @ToString.Include
    private String collection;
    private List<Map<String, Object>> items = new ArrayList<>();

    @ToString.Include(name = "itemCount")
    private int itemCount() {
        return items == null ? 0 : items.size();
    }
}
