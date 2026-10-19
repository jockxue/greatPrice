package com.greatprice.dto.admin;

import java.util.List;

public record TaobaoSearchResponse(
        String apiMethod,
        String requestId,
        long total,
        List<TaobaoItemResponse> items) {
}
