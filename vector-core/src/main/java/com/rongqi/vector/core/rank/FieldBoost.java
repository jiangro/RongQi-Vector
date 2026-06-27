package com.rongqi.vector.core.rank;

/**
 * 字段加权配置。
 *
 * <p>Ranker 会读取命中文档中的数字字段，并按字段值乘以权重叠加到原始向量分数上。</p>
 */
public class FieldBoost {
    private final String field;
    private final double weight;

    /**
     * 创建字段加权配置。
     *
     * @param field 参与排序的字段名
     * @param weight 字段权重，正数提高排名，负数降低排名
     */
    public FieldBoost(String field, double weight) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field 不能为空");
        }
        this.field = field.trim();
        this.weight = weight;
    }

    public String getField() {
        return field;
    }

    public double getWeight() {
        return weight;
    }
}
