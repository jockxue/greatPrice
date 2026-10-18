package com.greatprice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record PriceHistoryQuery(
        @Min(value = 1, message = "不能小于 1") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = 50, message = "不能超过 50") Integer size,
        @Positive(message = "必须为正整数") Long productId,
        @Positive(message = "必须为正整数") Long dealId) {

    public PriceHistoryQuery {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }
}
