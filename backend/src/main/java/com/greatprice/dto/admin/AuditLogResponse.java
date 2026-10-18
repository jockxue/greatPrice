package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorName,
        String action,
        String resourceType,
        Long resourceId,
        String detail,
        OffsetDateTime createdAt) {
}
