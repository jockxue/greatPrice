package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 好价录入。没有 status、审核人和核验人字段，客户端不能通过本对象改这些值。
 *
 * @param version 更新、提交审核和变更审核时必填；新建时忽略
 */
public record DealWriteRequest(
        @NotNull(message = "商品不能为空") @Positive(message = "商品必须为正整数") Long productId,
        @NotBlank(message = "当前价格不能为空") @Size(max = 16, message = "不能超过 16 个字符") String currentPrice,
        @Size(max = 16, message = "不能超过 16 个字符") String originalPrice,
        @Size(max = 3, message = "不能超过 3 个字符") String currency,
        @NotBlank(message = "优惠说明不能为空") @Size(max = 500, message = "不能超过 500 个字符") String dealDescription,
        @NotBlank(message = "来源类型不能为空") String sourceType,
        @NotBlank(message = "来源名称不能为空") @Size(max = 64, message = "不能超过 64 个字符") String sourceName,
        @Size(max = 1024, message = "不能超过 1024 个字符") String purchaseUrl,
        @Size(max = 1024, message = "不能超过 1024 个字符") String promotionUrl,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        Integer version) {
}
