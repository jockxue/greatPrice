package com.greatprice.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminDealResponse(
        Long id,
        Long productId,
        String productCode,
        String productStatus,
        boolean productDeleted,
        String title,
        String brand,
        String imageUrl,
        Long categoryId,
        String categoryName,
        String specification,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal originalPrice,
        String currency,
        String dealDescription,
        String sourceType,
        String sourceName,
        String purchaseUrl,
        String promotionUrl,
        boolean featured,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        OffsetDateTime verifiedAt,
        String status,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String rejectReason,
        String offlineReason,
        Long submittedBy,
        String submitterName,
        Long reviewedBy,
        String reviewerName,
        OffsetDateTime reviewedAt,
        int version,
        boolean demo,
        boolean expired) {
}
