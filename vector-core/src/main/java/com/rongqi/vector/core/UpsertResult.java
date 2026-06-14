package com.rongqi.vector.core;

/**
 * 写入结果。
 */
public class UpsertResult {
    private final int count;

    public UpsertResult(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}

