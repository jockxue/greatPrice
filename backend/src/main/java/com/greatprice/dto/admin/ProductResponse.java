package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record ProductResponse(
        Long id,
        String productCode,
        String title,
        String brand,
        String imageUrl,
        Long categoryId,
        String categoryName,
        String specification,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean demo) {
}
