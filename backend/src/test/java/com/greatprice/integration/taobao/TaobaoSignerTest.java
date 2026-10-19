package com.greatprice.integration.taobao;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaobaoSignerTest {

    @Test
    void md5MatchesOfficialExample() {
        assertEquals("66987CB115214E59E6EC978214934FB8", TaobaoSigner.sign(officialParams("md5"), "helloworld", TaobaoSigner.MD5));
    }

    @Test
    void hmacSha256UsesUpperHex() {
        String sign = TaobaoSigner.sign(officialParams(TaobaoSigner.HMAC_SHA256), "helloworld", TaobaoSigner.HMAC_SHA256);
        assertEquals("04DB15AD0774D5CFCE2C837DE43E3FCEA9011ED74F3038FB6AB5F3C4CEA119E8", sign);
        assertFalse(sign.contains("helloworld"));
    }

    @Test
    void timestampStaysUnencodedInsideSignature() {
        Map<String, String> params = officialParams(TaobaoSigner.HMAC_SHA256);
        assertEquals("2016-01-01 12:00:00", params.get("timestamp"));
        String encoded = TaobaoForm.encode(params);
        assertFalse(encoded.contains(" "));
        assertEquals(true, encoded.contains("2016-01-01+12%3A00%3A00") || encoded.contains("timestamp=2016-01-01+12%3A00%3A00"));
    }

    private static Map<String, String> officialParams(String signMethod) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "taobao.item.seller.get");
        params.put("app_key", "12345678");
        params.put("session", "test");
        params.put("timestamp", "2016-01-01 12:00:00");
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", signMethod);
        params.put("fields", "num_iid,title,nick,price,num");
        params.put("num_iid", "11223344");
        return params;
    }
}
