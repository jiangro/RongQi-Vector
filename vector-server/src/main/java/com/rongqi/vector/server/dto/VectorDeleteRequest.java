package com.rongqi.vector.server.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * HTTP 删除请求。
 *
 * <p>ids 用于按主键删除；filterObject 用于按对象条件删除。两者至少传一个。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class VectorDeleteRequest {
    @ToString.Include
    private String domain;
    @ToString.Include
    private String collection;
    private List<Object> ids = new ArrayList<>();
    private Map<String, Object> filterObject = new LinkedHashMap<>();

    @ToString.Include(name = "idsSize")
    private int idsSize() {
        return ids == null ? 0 : ids.size();
    }

    @ToString.Include(name = "filterFieldCount")
    private int filterFieldCount() {
        return filterObject == null ? 0 : filterObject.size();
    }
}
