package com.greatprice.integration.taobao;

public interface TaobaoTransport {

    TaobaoHttpResponse post(String url, String formBody, int connectTimeoutMs, int readTimeoutMs);
}
