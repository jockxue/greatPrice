package com.greatprice.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceHistoryResponse(
        Long id,
        Long productId,
        String productCode,
        String productTitle,
        Long dealId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal price,
        String currency,
        String sourceType,
        String sourceName,
        String verificationStatus,
        OffsetDateTime recordedAt,
        Long verifiedBy,
        String verifierName,
        String note) {
}
