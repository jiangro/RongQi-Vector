package com.rongqi.vector.core.text;

/**
 * 文本切块参数。
 *
 * <p>默认 800 字符一段、100 字符重叠，适合先做稳定的通用切分；业务方可以按模型上下文长度自行调整。</p>
 */
public class TextChunkOptions {
    private final int maxChars;
    private final int overlapChars;

    private TextChunkOptions(Builder builder) {
        this.maxChars = builder.maxChars;
        this.overlapChars = builder.overlapChars;
    }

    public static TextChunkOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxChars() {
        return maxChars;
    }

    public int getOverlapChars() {
        return overlapChars;
    }

    /**
     * TextChunkOptions 构建器。
     */
    public static class Builder {
        private int maxChars = 800;
        private int overlapChars = 100;

        /**
         * 设置单个分片最大字符数。
         */
        public Builder maxChars(int maxChars) {
            this.maxChars = maxChars;
            return this;
        }

        /**
         * 设置相邻分片之间的重叠字符数。
         */
        public Builder overlapChars(int overlapChars) {
            this.overlapChars = overlapChars;
            return this;
        }

        public TextChunkOptions build() {
            if (maxChars <= 0) {
                throw new IllegalArgumentException("maxChars 必须大于 0");
            }
            if (overlapChars < 0) {
                throw new IllegalArgumentException("overlapChars 不能小于 0");
            }
            if (overlapChars >= maxChars) {
                throw new IllegalArgumentException("overlapChars 必须小于 maxChars");
            }
            return new TextChunkOptions(this);
        }
    }
}
