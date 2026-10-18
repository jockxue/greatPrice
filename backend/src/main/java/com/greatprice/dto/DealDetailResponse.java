package com.greatprice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 好价详情。
 *
 * @param originalPrice 参考价格，可为空；只用于展示，不代表经过核验的优惠幅度
 * @param sourceType    DEMO 表示演示数据，前端需明确提示
 * @param startsAt      促销开始时间，为 null 表示未限定
 * @param expiresAt     促销结束时间，为 null 表示未知或长期有效
 * @param purchaseUrl   普通购买链接，仅在为合法 https 地址时返回，否则为 null
 * @param promotionUrl  推广链接，仅在已配置且为合法 https 地址时返回，否则为 null
 */
public record DealDetailResponse(
        Long id,
        Long productId,
        String title,
        String brand,
        String imageUrl,
        Long categoryId,
        String categoryName,
        String specification,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal originalPrice,
        String currency,
        String dealDescription,
        String sourceType,
        String sourceName,
        OffsetDateTime startsAt,
        OffsetDateTime expiresAt,
        OffsetDateTime verifiedAt,
        OffsetDateTime publishedAt,
        String purchaseUrl,
        String promotionUrl) {
}
