package com.greatprice.dto.admin;

public record TaobaoProbeResponse(
        String state,
        String apiMethod,
        String errorCode,
        String message,
        String requestId,
        boolean retryable) {
}
