package com.greatprice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 好价列表项（商品卡片）。
 *
 * @param originalPrice 参考价格，可为空；只用于展示，不代表经过核验的优惠幅度
 * @param sourceType    DEMO 表示演示数据，前端需明确提示
 * @param verifiedAt    最近核验时间，未核验时为 null
 */
public record DealSummaryResponse(
        Long id,
        String title,
        String brand,
        String imageUrl,
        Long categoryId,
        String categoryName,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal originalPrice,
        String currency,
        String dealDescription,
        String sourceType,
        String sourceName,
        OffsetDateTime expiresAt,
        OffsetDateTime verifiedAt,
        OffsetDateTime publishedAt) {
}
