package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record DealReviewResponse(
        Long id,
        Long dealId,
        String action,
        Long reviewerId,
        String reviewerName,
        String reason,
        OffsetDateTime createdAt) {
}
