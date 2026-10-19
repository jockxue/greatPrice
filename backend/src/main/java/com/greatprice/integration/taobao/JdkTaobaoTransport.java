package com.greatprice.integration.taobao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class JdkTaobaoTransport implements TaobaoTransport {

    @Override
    public TaobaoHttpResponse post(String url, String formBody, int connectTimeoutMs, int readTimeoutMs) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1)))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 1)))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            boolean rateLimited = response.statusCode() == 429;
            if (response.statusCode() >= 500 || rateLimited) {
                throw new TaobaoTransportException(
                        rateLimited ? "淘宝联盟限流" : "淘宝联盟服务暂时不可用",
                        false,
                        rateLimited);
            }
            return new TaobaoHttpResponse(response.statusCode(), response.body());
        } catch (HttpTimeoutException ex) {
            throw new TaobaoTransportException("淘宝联盟请求超时", true, false);
        } catch (IOException ex) {
            throw new TaobaoTransportException("淘宝联盟网络错误", false, false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TaobaoTransportException("淘宝联盟请求被中断", false, false);
        }
    }
}
