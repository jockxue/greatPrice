package com.greatprice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaobaoSearchQuery(
        @NotBlank(message = "关键词不能为空") @Size(max = 60, message = "不能超过 60 个字符") String keyword,
        @Min(value = 1, message = "不能小于 1") @Max(value = 100, message = "不能超过 100") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = 20, message = "不能超过 20") Integer size,
        @Size(max = 16, message = "不能超过 16 个字符") String startPrice,
        @Size(max = 16, message = "不能超过 16 个字符") String endPrice,
        @Size(max = 32, message = "不能超过 32 个字符") String sort,
        Boolean hasCoupon) {

    public TaobaoSearchQuery {
        keyword = keyword == null ? null : keyword.strip();
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
        startPrice = blank(startPrice);
        endPrice = blank(endPrice);
        sort = blank(sort);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
