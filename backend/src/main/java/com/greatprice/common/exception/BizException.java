package com.greatprice.common.exception;

import com.greatprice.common.api.ErrorCode;

/**
 * 可预期的业务异常，由 {@link GlobalExceptionHandler} 转换为统一响应，message 会直接展示给用户。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
