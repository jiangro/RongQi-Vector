package com.rongqi.vector.core.rank;

import com.rongqi.vector.core.SearchHit;
import com.rongqi.vector.core.SearchOptions;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 搜索结果二次排序工具。
 *
 * <p>该类只做内存排序，不依赖 Milvus 或 HTTP 层，便于 Maven 注解模式和 HTTP collection 模式复用。</p>
 */
public class Ranker {

    /**
     * 根据 SearchOptions 对命中结果进行二次排序并按 topK 截断。
     *
     * @param hits Milvus 返回的候选结果
     * @param options 搜索配置
     * @param <T> 命中实体类型
     * @return 排序后的结果
     */
    public <T> List<SearchHit<T>> rank(List<SearchHit<T>> hits, SearchOptions options) {
        if (hits == null || hits.isEmpty()) {
            return new ArrayList<>();
        }
        SearchOptions actualOptions = options == null ? SearchOptions.topK(10) : options;
        RankOptions rankOptions = actualOptions.getRankOptions();
        if (rankOptions == null || !rankOptions.hasFieldBoosts()) {
            return limit(hits, actualOptions.getTopK());
        }
        List<SearchHit<T>> ranked = new ArrayList<>();
        for (SearchHit<T> hit : hits) {
            double rankScore = hit.getScore();
            for (FieldBoost boost : rankOptions.getFieldBoosts()) {
                Number value = readNumber(hit.getEntity(), boost.getField());
                if (value != null) {
                    rankScore += value.doubleValue() * boost.getWeight();
                }
            }
            ranked.add(new SearchHit<>(hit.getScore(), rankScore, hit.getEntity()));
        }
        ranked.sort(Comparator.comparing(SearchHit<T>::getRankScore).reversed());
        return limit(ranked, actualOptions.getTopK());
    }

    private <T> List<SearchHit<T>> limit(List<SearchHit<T>> hits, int topK) {
        int size = Math.min(Math.max(1, topK), hits.size());
        return new ArrayList<>(hits.subList(0, size));
    }

    @SuppressWarnings("unchecked")
    private Number readNumber(Object entity, String fieldName) {
        Object value = readValue(entity, fieldName);
        if (value instanceof Number) {
            return (Number) value;
        }
        return null;
    }

    /**
     * 读取 Map、JavaBean getter 或普通字段中的值，兼容 HTTP collection 和 Maven domain 两种实体。
     */
    private Object readValue(Object entity, String fieldName) {
        if (entity == null || fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }
        if (entity instanceof Map) {
            return ((Map<?, ?>) entity).get(fieldName);
        }
        Object getterValue = readGetter(entity, fieldName);
        if (getterValue != null) {
            return getterValue;
        }
        return readField(entity, fieldName);
    }

    private Object readGetter(Object entity, String fieldName) {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String[] methodNames = new String[]{"get" + suffix, "is" + suffix};
        for (String methodName : methodNames) {
            try {
                Method method = entity.getClass().getMethod(methodName);
                return method.invoke(entity);
            } catch (ReflectiveOperationException ignored) {
                // 当前实体没有该 getter 时继续尝试其他读取方式。
            }
        }
        return null;
    }

    private Object readField(Object entity, String fieldName) {
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(entity);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }
}
