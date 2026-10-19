package com.greatprice.dto.admin;

public record TaobaoPromotionResponse(
        String apiMethod,
        String requestId,
        String itemId,
        String promotionUrl,
        String linkField,
        String couponInfo,
        String couponStart,
        String couponEnd,
        boolean persisted,
        Long dealId,
        boolean miniProgramCompatible,
        String clientNote) {
}
