package com.greatprice.dto.admin;

public record TaobaoImportResponse(
        String productOutcome,
        Long productId,
        String productCode,
        String dealOutcome,
        Long dealId,
        String dealStatus,
        String message) {
}
