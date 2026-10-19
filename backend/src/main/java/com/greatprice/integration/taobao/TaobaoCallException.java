package com.greatprice.integration.taobao;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;

/** 淘宝调用失败。message 已脱敏，platformCode 只用于任务记录。 */
public class TaobaoCallException extends BizException {

    private final String platformCode;
    private final boolean retryable;

    public TaobaoCallException(ErrorCode errorCode, String message, String platformCode, boolean retryable) {
        super(errorCode, message);
        this.platformCode = platformCode;
        this.retryable = retryable;
    }

    public String platformCode() {
        return platformCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
