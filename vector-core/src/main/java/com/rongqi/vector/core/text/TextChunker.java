package com.rongqi.vector.core.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用文本切块工具。
 *
 * <p>该工具只负责按字符长度切分文本，不依赖 Milvus、HTTP 或具体 EmbeddingProvider，方便不同入口复用。</p>
 */
public final class TextChunker {
    private TextChunker() {
    }

    /**
     * 使用默认参数切分文本。
     *
     * @param text 原始文本
     * @return 文本分片列表
     */
    public static List<TextChunk> split(String text) {
        return split(text, TextChunkOptions.defaults());
    }

    /**
     * 按指定参数切分文本。
     *
     * @param text 原始文本
     * @param options 切块参数，为空时使用默认参数
     * @return 文本分片列表
     */
    public static List<TextChunk> split(String text, TextChunkOptions options) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        TextChunkOptions actualOptions = options == null ? TextChunkOptions.defaults() : options;
        int maxChars = actualOptions.getMaxChars();
        int overlapChars = actualOptions.getOverlapChars();
        List<TextChunk> chunks = new ArrayList<>();
        int start = firstContentIndex(text, 0);
        int index = 0;
        while (start < text.length()) {
            int hardEnd = Math.min(start + maxChars, text.length());
            int end = hardEnd == text.length() ? hardEnd : chooseBreak(text, start, hardEnd);
            int contentStart = firstContentIndex(text, start);
            int contentEnd = lastContentIndex(text, end);
            if (contentStart < contentEnd) {
                // 记录原文位置时使用去掉首尾空白后的范围，便于业务方准确回溯内容。
                chunks.add(new TextChunk(index++, text.substring(contentStart, contentEnd), contentStart, contentEnd));
            }
            if (hardEnd == text.length()) {
                break;
            }
            int nextStart = Math.max(end - overlapChars, start + 1);
            start = firstContentIndex(text, nextStart);
        }
        return chunks;
    }

    /**
     * 优先在换行处切分，其次在空白字符处切分；找不到合适边界时按最大长度硬切。
     */
    private static int chooseBreak(String text, int start, int hardEnd) {
        int lineBreak = findLastBreak(text, start, hardEnd, '\n');
        if (lineBreak > start) {
            return lineBreak;
        }
        for (int i = hardEnd - 1; i > start; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return hardEnd;
    }

    /**
     * 查找指定切分字符的最后位置。
     */
    private static int findLastBreak(String text, int start, int end, char breakChar) {
        for (int i = end - 1; i > start; i--) {
            if (text.charAt(i) == breakChar) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 跳过起始位置之后的空白字符。
     */
    private static int firstContentIndex(String text, int start) {
        int index = Math.max(0, start);
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * 去掉结束位置之前的空白字符。
     */
    private static int lastContentIndex(String text, int end) {
        int index = Math.min(end, text.length());
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return index;
    }
}
