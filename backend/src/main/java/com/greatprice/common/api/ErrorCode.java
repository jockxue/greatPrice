package com.greatprice.common.api;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。code 前三位与 HTTP 状态码一致，便于排查；新增错误码请同步更新 docs/api-conventions.md。
 */
public enum ErrorCode {

    SUCCESS(0, "成功", HttpStatus.OK),

    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    DEAL_UNAVAILABLE(40401, "好价已下架或已过期", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(40900, "数据已被他人更新，请刷新后重试", HttpStatus.CONFLICT),

    INTERNAL_ERROR(50000, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    UPSTREAM_ERROR(50200, "上游服务调用失败", HttpStatus.BAD_GATEWAY),
    UPSTREAM_UNAVAILABLE(50300, "上游服务暂不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
