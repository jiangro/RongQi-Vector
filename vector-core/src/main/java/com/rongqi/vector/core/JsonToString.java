package com.rongqi.vector.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 用于结果对象 toString 的 JSON 序列化工具。
 *
 * <p>统一使用 Gson 输出 JSON，避免业务 domain 没有重写 toString 时出现“类名@hash”。</p>
 */
final class JsonToString {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private JsonToString() {
    }

    static String toJson(Object value) {
        return GSON.toJson(value);
    }
}
