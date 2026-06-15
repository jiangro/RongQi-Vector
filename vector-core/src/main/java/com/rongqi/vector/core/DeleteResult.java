package com.rongqi.vector.core;

/**
 * 删除结果。
 */
public class DeleteResult {
    private final int count;

    public DeleteResult(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return JsonToString.toJson(this);
    }
}
