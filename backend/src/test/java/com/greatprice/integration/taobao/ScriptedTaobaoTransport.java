package com.greatprice.integration.taobao;

import java.util.concurrent.atomic.AtomicInteger;

public class ScriptedTaobaoTransport implements TaobaoTransport {

    public static final AtomicInteger calls = new AtomicInteger();
    public static volatile String searchBody = "{}";
    public static volatile String detailBody = "{}";
    public static volatile String promotionBody = "{}";
    public static volatile TaobaoTransportException failure;

    public static void reset() {
        calls.set(0);
        searchBody = "{}";
        detailBody = "{}";
        promotionBody = "{}";
        failure = null;
    }

    @Override
    public TaobaoHttpResponse post(String url, String formBody, int connectTimeoutMs, int readTimeoutMs) {
        calls.incrementAndGet();
        if (failure != null) {
            throw failure;
        }
        if (formBody.contains("taobao.tbk.privilege.get")) {
            return new TaobaoHttpResponse(200, promotionBody);
        }
        if (formBody.contains("taobao.tbk.item.info.get")) {
            return new TaobaoHttpResponse(200, detailBody);
        }
        return new TaobaoHttpResponse(200, searchBody);
    }
}
