package com.greatprice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AuditQuery(
        @Min(value = 1, message = "不能小于 1") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = 50, message = "不能超过 50") Integer size,
        @Size(max = 32, message = "不能超过 32 个字符") String action,
        @Size(max = 32, message = "不能超过 32 个字符") String resourceType) {

    public AuditQuery {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        action = action == null || action.isBlank() ? null : action.strip();
        resourceType = resourceType == null || resourceType.isBlank() ? null : resourceType.strip();
    }
}
