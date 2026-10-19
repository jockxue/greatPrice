package com.greatprice.integration.taobao;

public class TaobaoTransportException extends RuntimeException {

    private final boolean timeout;
    private final boolean rateLimited;

    public TaobaoTransportException(String message, boolean timeout, boolean rateLimited) {
        super(message);
        this.timeout = timeout;
        this.rateLimited = rateLimited;
    }

    public boolean timeout() {
        return timeout;
    }

    public boolean rateLimited() {
        return rateLimited;
    }
}
