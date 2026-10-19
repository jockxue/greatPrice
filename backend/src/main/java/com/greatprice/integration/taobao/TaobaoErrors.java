package com.greatprice.integration.taobao;

import com.greatprice.common.api.ErrorCode;

import java.util.Set;

/**
 * 可重试错误只收录三个目标 API 官方错误表里写明可以稍后重试的码，以及 HTTP 429/5xx。
 * 参数错误、无权限、授权失效不重试。
 */
public final class TaobaoErrors {

    private static final Set<String> RETRYABLE = Set.of(
            "24",
            "40001",
            "isp.special-campaign-error",
            "isp.entry-sys-error",
            "isp.sys-error",
            "isp.pid-check-error",
            "mrc-service-unavailable",
            "isp.isp.mrc-service-unavailable");

    private TaobaoErrors() {
    }

    public static boolean retryable(String code, String subCode) {
        return RETRYABLE.contains(code) || RETRYABLE.contains(subCode);
    }

    public static boolean emptyResult(String code, String subCode) {
        return "50001".equals(code) || "50001".equals(subCode);
    }

    public static ErrorCode toErrorCode(boolean transportFailure, String code, String subCode) {
        if (transportFailure || retryable(code, subCode)) {
            return ErrorCode.UPSTREAM_UNAVAILABLE;
        }
        return ErrorCode.UPSTREAM_ERROR;
    }
}
