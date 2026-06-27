package com.rongqi.vector.core.text;

/**
 * 文本分片结果。
 *
 * <p>每个分片保留原文中的起止位置，方便业务方生成 chunk_id、回溯原文或排查 embedding 数据。</p>
 */
public class TextChunk {
    private final int index;
    private final String content;
    private final int start;
    private final int end;

    /**
     * 创建文本分片。
     *
     * @param index 分片序号，从 0 开始
     * @param content 分片文本内容
     * @param start 分片在原文中的起始下标，包含该位置
     * @param end 分片在原文中的结束下标，不包含该位置
     */
    public TextChunk(int index, String content, int start, int end) {
        this.index = index;
        this.content = content;
        this.start = start;
        this.end = end;
    }

    public int getIndex() {
        return index;
    }

    public String getContent() {
        return content;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
