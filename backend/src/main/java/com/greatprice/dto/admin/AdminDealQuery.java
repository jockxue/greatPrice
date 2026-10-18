package com.greatprice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminDealQuery(
        @Min(value = 1, message = "不能小于 1") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = 50, message = "不能超过 50") Integer size,
        @Positive(message = "必须为正整数") Long categoryId,
        @Positive(message = "必须为正整数") Long productId,
        @Size(max = 50, message = "不能超过 50 个字符") String keyword,
        String status,
        String sourceType,
        AdminDealSort sortBy) {

    public AdminDealQuery {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        status = blankToNull(status);
        sourceType = blankToNull(sourceType);
        sortBy = sortBy == null ? AdminDealSort.CREATED_AT_DESC : sortBy;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
