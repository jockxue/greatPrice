package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record TaobaoJobResponse(
        Long id,
        String jobName,
        String status,
        int requestedCount,
        int processedCount,
        int failedCount,
        String errorCode,
        boolean retryable,
        String message,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt) {
}
