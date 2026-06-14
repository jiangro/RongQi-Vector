package com.rongqi.vector.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * domain 诊断结果，用于帮助新手快速定位配置问题。
 */
public class VectorDiagnosis {
    private final boolean healthy;
    private final List<String> messages;

    public VectorDiagnosis(boolean healthy, List<String> messages) {
        this.healthy = healthy;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public boolean isHealthy() {
        return healthy;
    }

    public List<String> getMessages() {
        return messages;
    }
}

