package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String displayName,
        String role,
        String status,
        OffsetDateTime createdAt) {
}
