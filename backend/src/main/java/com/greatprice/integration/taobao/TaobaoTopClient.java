package com.greatprice.integration.taobao;

import tools.jackson.databind.JsonNode;
import com.greatprice.common.api.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaobaoTopClient {

    private static final Logger log = LoggerFactory.getLogger(TaobaoTopClient.class);

    private final TaobaoProperties properties;
    private final TaobaoTransport transport;
    private final TaobaoClock clock;
    private final TaobaoRetryDelay delay;

    public TaobaoTopClient(TaobaoProperties properties, TaobaoTransport transport, TaobaoClock clock, TaobaoRetryDelay delay) {
        this.properties = properties;
        this.transport = transport;
        this.clock = clock;
        this.delay = delay;
    }

    public JsonNode call(String method, Map<String, String> business, boolean includeSession) {
        int attempts = properties.extraRetries() + 1;
        TaobaoTransportException lastTransport = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode body = post(method, business, includeSession);
                TaobaoPayloads.Failure failure = TaobaoPayloads.failure(body);
                if (failure != null && failure.retryable() && attempt < attempts) {
                    log.warn("淘宝联盟将重试 method={} code={} subCode={} requestId={}",
                            method, failure.code(), failure.subCode(), failure.requestId());
                    delay.pause(attempt, "24".equals(failure.code()) || "24".equals(failure.subCode()));
                    continue;
                }
                return body;
            } catch (TaobaoTransportException ex) {
                lastTransport = ex;
                if (attempt == attempts) {
                    break;
                }
                log.warn("淘宝联盟传输失败将重试 method={} timeout={} rateLimited={}", method, ex.timeout(), ex.rateLimited());
                delay.pause(attempt, ex.rateLimited());
            }
        }
        String message = lastTransport != null && lastTransport.timeout()
                ? "淘宝联盟请求超时"
                : lastTransport != null && lastTransport.rateLimited() ? "淘宝联盟限流，请稍后重试" : "淘宝联盟网络错误";
        throw new TaobaoCallException(ErrorCode.UPSTREAM_UNAVAILABLE, message, null, true);
    }

    private JsonNode post(String method, Map<String, String> business, boolean includeSession) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", method);
        params.put("app_key", properties.getAppKey().strip());
        params.put("timestamp", clock.timestamp());
        params.put("v", "2.0");
        params.put("sign_method", TaobaoSigner.HMAC_SHA256);
        params.put("format", "json");
        if (includeSession) {
            params.put("session", properties.getSession().strip());
        }
        business.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                params.put(key, value.strip());
            }
        });
        params.put("sign", TaobaoSigner.sign(params, properties.getAppSecret().strip(), TaobaoSigner.HMAC_SHA256));
        String body = TaobaoForm.encode(params);
        TaobaoHttpResponse response = transport.post(
                properties.getGatewayUrl().strip(),
                body,
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs());
        return TaobaoPayloads.readTree(response.body());
    }
}
