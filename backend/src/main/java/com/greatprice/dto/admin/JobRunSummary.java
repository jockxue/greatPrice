package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record JobRunSummary(
        String jobName,
        String status,
        int processedCount,
        int failedCount,
        String message,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt) {
}
