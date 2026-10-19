package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 核验人和核验时间由服务端决定，请求体里没有这两个字段。 */
public record PriceVerifyRequest(
        @NotNull(message = "商品不能为空") @Positive(message = "商品必须为正整数") Long productId,
        @Positive(message = "好价必须为正整数") Long dealId,
        @NotBlank(message = "核验价格不能为空") @Size(max = 16, message = "不能超过 16 个字符") String price,
        @NotBlank(message = "核验来源不能为空") String sourceType,
        @NotBlank(message = "来源名称不能为空") @Size(max = 64, message = "不能超过 64 个字符") String sourceName,
        @Size(max = 500, message = "不能超过 500 个字符") String note,
        @Size(max = 64, message = "不能超过 64 个字符") String idempotencyKey) {
}
