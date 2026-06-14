package com.rongqi.vector.server.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HTTP 删除请求。
 *
 * <p>ids 用于按主键删除；filterObject 用于按对象条件删除。两者至少传一个。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class VectorDeleteRequest {
    private String domain;
    private String collection;
    private List<Object> ids = new ArrayList<>();
    private Map<String, Object> filterObject = new LinkedHashMap<>();
}
