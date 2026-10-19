package com.greatprice.integration.taobao;

import com.greatprice.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaobaoPayloadTest {

    @Test
    void parsesOfficialSearchShapeAndKeepsMissingCouponEmpty() {
        String json = """
                {"tbk_dg_material_optional_response":{"total_results":1,"request_id":"req-search","result_list":{"map_data":[{
                  "item_id":"5678899993","num_iid":"556633720749","title":"纸巾","pict_url":"https://img.alicdn.com/a.jpg",
                  "reserve_price":"102.00","zk_final_price":"88.00","shop_title":"测试店","item_url":"https://item.taobao.com/item.htm?id=1"
                }]}}}
                """;
        TaobaoPayloads.SearchPage page = TaobaoPayloads.search(TaobaoPayloads.readTree(json));
        assertEquals("req-search", page.requestId());
        assertEquals("5678899993", page.hits().get(0).itemId());
        assertNull(page.hits().get(0).couponId());
        assertNull(page.hits().get(0).couponInfo());
        assertEquals("88.00", TaobaoMoney.parse(page.hits().get(0).finalPrice()).value());
    }

    @Test
    void illegalMoneyIsNotAPrice() {
        assertEquals(TaobaoMoney.INVALID, TaobaoMoney.parse("1550表示15.5%").status());
        assertEquals(TaobaoMoney.INVALID, TaobaoMoney.parse("88.001").status());
        assertEquals(TaobaoMoney.MISSING, TaobaoMoney.parse(" ").status());
        assertNull(TaobaoMoney.parse("-1").value());
    }

    @Test
    void officialErrorAndEmptyResult() {
        String error = """
                {"error_response":{"code":50,"msg":"Remote service error","sub_code":"isv.invalid-parameter","sub_msg":"非法参数 super-secret-value","request_id":"err-1"}}
                """;
        TaobaoPayloads.Failure failure = TaobaoPayloads.failure(TaobaoPayloads.readTree(error));
        assertEquals("50", failure.code());
        assertFalse(failure.retryable());
        assertEquals("***", TaobaoTexts.redact(failure.subMsg(), "super-secret-value", "session-token-value").contains("super-secret-value") ? "visible" : "***");
        String empty = """
                {"error_response":{"code":50001,"sub_msg":"当前条件查询无结果"}}
                """;
        TaobaoPayloads.SearchPage page = TaobaoPayloads.search(TaobaoPayloads.readTree(empty));
        assertTrue(page.hits().isEmpty());
        assertNull(page.failure());
    }

    @Test
    void detailAndPrivilegeFields() {
        String detail = """
                {"tbk_item_info_get_response":{"results":{"n_tbk_item":[{"num_iid":"123","title":"商品","zk_final_price":"not-a-price","sale_price":"16.50","nick":"店"}]}}}
                """;
        TaobaoPayloads.Detail parsed = TaobaoPayloads.detail(TaobaoPayloads.readTree(detail), "123");
        assertEquals("123", parsed.item().itemId());
        assertEquals(TaobaoMoney.INVALID, TaobaoMoney.parse(parsed.item().finalPrice()).status());
        assertEquals("16.50", TaobaoMoney.parse(parsed.item().salePrice()).value());
        String link = """
                {"tbk_privilege_get_response":{"request_id":"link-1","result":{"data":{"item_id":"123","coupon_info":"满16元减10元","coupon_click_url":"https://uland.taobao.com/coupon/edetail?e=1"}}}}
                """;
        TaobaoPayloads.Promotion promotion = TaobaoPayloads.promotion(TaobaoPayloads.readTree(link));
        assertEquals("https://uland.taobao.com/coupon/edetail?e=1", promotion.couponClickUrl());
        assertNull(promotion.itemUrl());
    }

    @Test
    void brokenJsonAndBadPromotionLink() {
        assertThrows(BizException.class, () -> TaobaoPayloads.readTree("not-json"));
        assertThrows(BizException.class, () -> TaobaoLinks.promotion("javascript:alert(1)"));
        assertNull(TaobaoLinks.promotion(" "));
        assertNull(TaobaoLinks.image("uland.xxx"));
        assertEquals("https://img.alicdn.com/a.jpg", TaobaoLinks.image("//img.alicdn.com/a.jpg"));
    }

    @Test
    void productCodeNeverLooksLikeDemo() {
        assertEquals("TB-556633720749", TaobaoIds.productCode("556633720749"));
        assertFalse(TaobaoIds.productCode("a".repeat(80)).startsWith("DEMO-"));
        assertTrue(TaobaoIds.productCode("a".repeat(80)).length() <= 64);
        assertEquals("LISTING:556633720749", TaobaoIds.offerKey("556633720749"));
        assertThrows(BizException.class, () -> TaobaoIds.requireItemId("../1"));
    }

    @Test
    void clientDoesNotPutSecretInRequestAndRetriesRateLimitOnly() {
        TaobaoProperties properties = new TaobaoProperties();
        properties.setAppKey("test-app-key");
        properties.setAppSecret("super-secret-value");
        properties.setSession("session-token-value");
        properties.setGatewayUrl("https://eco.taobao.com/router/rest");
        properties.setMaxRetries(1);
        RecordingTransport transport = new RecordingTransport();
        CountingDelay delay = new CountingDelay();
        TaobaoTopClient client = new TaobaoTopClient(properties, transport, () -> "2016-01-01 12:00:00", delay);
        transport.body = """
                {"error_response":{"code":24,"sub_code":"24","sub_msg":"当前memberId触发QPS限流","request_id":"limit-1"}}
                """;
        client.call(TaobaoPayloads.PROMOTION_METHOD, Map.of("item_id", "123"), true);
        assertEquals(2, transport.calls);
        assertEquals(1, delay.pauses);
        assertFalse(transport.lastBody.contains("super-secret-value"));
        assertTrue(transport.lastBody.contains("sign="));
        assertTrue(transport.lastBody.contains("session-token-value"));
        transport.calls = 0;
        transport.body = """
                {"error_response":{"code":50,"sub_code":"isv.invalid-parameter","sub_msg":"非法参数","request_id":"bad-1"}}
                """;
        client.call(TaobaoPayloads.SEARCH_METHOD, Map.of("q", "纸巾"), false);
        assertEquals(1, transport.calls);
        assertFalse(transport.lastBody.contains("session-token-value"));
    }

    @Test
    void timeoutIsRetryableAndAuthorizationFailureIsNot() {
        TaobaoProperties properties = new TaobaoProperties();
        properties.setAppKey("test-app-key");
        properties.setAppSecret("super-secret-value");
        properties.setGatewayUrl("https://eco.taobao.com/router/rest");
        properties.setMaxRetries(1);
        RecordingTransport transport = new RecordingTransport();
        transport.failure = new TaobaoTransportException("淘宝联盟请求超时", true, false);
        TaobaoTopClient client = new TaobaoTopClient(properties, transport, () -> "2016-01-01 12:00:00", (attempt, rateLimited) -> { });
        TaobaoCallException timeout = assertThrows(TaobaoCallException.class,
                () -> client.call(TaobaoPayloads.DETAIL_METHOD, Map.of("num_iids", "1"), false));
        assertTrue(timeout.retryable());
        assertEquals(2, transport.calls);
        assertFalse(timeout.getMessage().contains("super-secret-value"));
    }

    private static final class RecordingTransport implements TaobaoTransport {
        String body = "{}";
        String lastBody = "";
        int calls;
        TaobaoTransportException failure;

        @Override
        public TaobaoHttpResponse post(String url, String formBody, int connectTimeoutMs, int readTimeoutMs) {
            calls++;
            lastBody = formBody;
            if (failure != null) {
                throw failure;
            }
            return new TaobaoHttpResponse(200, body);
        }
    }

    private static final class CountingDelay implements TaobaoRetryDelay {
        int pauses;

        @Override
        public void pause(int attempt, boolean rateLimited) {
            pauses++;
        }
    }
}
