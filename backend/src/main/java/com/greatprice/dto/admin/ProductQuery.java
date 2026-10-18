package com.greatprice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductQuery(
        @Min(value = 1, message = "不能小于 1") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = 50, message = "不能超过 50") Integer size,
        @Positive(message = "必须为正整数") Long categoryId,
        String status,
        @Size(max = 50, message = "不能超过 50 个字符") String keyword) {

    public ProductQuery {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        status = status == null || status.isBlank() ? null : status.strip();
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
    }
}
