package com.greatprice.controller;

import com.greatprice.integration.taobao.ScriptedTaobaoTransport;
import com.greatprice.integration.taobao.TaobaoProperties;
import com.greatprice.integration.taobao.TaobaoTransport;
import com.greatprice.integration.taobao.TaobaoTransportException;
import com.greatprice.security.LoginAttemptGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TaobaoAffiliateIntegrationTest.FakeTransportConfig.class)
class TaobaoAffiliateIntegrationTest {

    private static final String PASSWORD = "Admin#Test12345";
    private static final String SECRET = "test-app-secret";
    private static final String SESSION_KEY = "test-session-token";
    private static final String ITEM_ID = "556633720749";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:taobao_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("app.taobao.enabled", () -> "true");
        registry.add("app.taobao.manual-sync-enabled", () -> "true");
        registry.add("app.taobao.app-key", () -> "test-app-key");
        registry.add("app.taobao.app-secret", () -> SECRET);
        registry.add("app.taobao.adzone-id", () -> "12345678");
        registry.add("app.taobao.site-id", () -> "22334455");
        registry.add("app.taobao.session", () -> SESSION_KEY);
        registry.add("app.taobao.gateway-url", () -> "https://eco.taobao.com/router/rest");
        registry.add("app.taobao.max-retries", () -> "0");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private LoginAttemptGuard loginAttemptGuard;
    @Autowired
    private TaobaoProperties taobaoProperties;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        ScriptedTaobaoTransport.reset();
        ScriptedTaobaoTransport.searchBody = searchJson();
        ScriptedTaobaoTransport.detailBody = detailJson(ITEM_ID, "9.90");
        ScriptedTaobaoTransport.promotionBody = promotionJson("https://uland.taobao.com/coupon/edetail?e=abc");
        loginAttemptGuard.reset();
        taobaoProperties.setEnabled(true);
        taobaoProperties.setManualSyncEnabled(true);
        jdbc.update("DELETE FROM deal WHERE external_offer_key LIKE 'LISTING:%'");
        jdbc.update("DELETE FROM product_external_ref");
        jdbc.update("DELETE FROM product WHERE product_code LIKE 'TB-%'");
        jdbc.update("DELETE FROM job_run WHERE job_name LIKE 'TAOBAO_%'");
        jdbc.update("DELETE FROM admin_user");
        jdbc.update("INSERT INTO admin_user (username, password_hash, display_name, role, status, failed_login_count) VALUES (?, ?, '管理员', 'ADMIN', 'ACTIVE', 0)",
                "admin", passwordEncoder.encode(PASSWORD));
        session = login();
    }

    @Test
    void statusHidesSecretsAndAnonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/v1/affiliate/taobao/status"))
                .andExpect(status().isUnauthorized());
        MvcResult result = mockMvc.perform(get("/api/admin/v1/affiliate/taobao/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appSecretConfigured").value(true))
                .andExpect(jsonPath("$.data.sessionConfigured").value(true))
                .andExpect(jsonPath("$.data.searchConfigured").value(true))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(SECRET));
        assertFalse(body.contains(SESSION_KEY));
    }

    @Test
    void searchDetailAndImportStayDraftWithoutPriceHistory() throws Exception {
        int history = historyCount();
        String demoTitle = jdbc.queryForObject("SELECT title FROM product WHERE id = 2", String.class);
        mockMvc.perform(get("/api/admin/v1/affiliate/taobao/items").session(session).param("keyword", "纸巾"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].itemId").value(ITEM_ID))
                .andExpect(jsonPath("$.data.items[0].finalPrice").value("9.90"))
                .andExpect(jsonPath("$.data.items[0].promotionLinkGenerated").value(false))
                .andExpect(jsonPath("$.data.items[0].couponId").value("coupon-1"));

        mockMvc.perform(get("/api/admin/v1/affiliate/taobao/items/detail").session(session).param("itemId", ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("测试纸巾"));

        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/imports").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + ITEM_ID + "\",\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productOutcome").value("CREATED"))
                .andExpect(jsonPath("$.data.dealOutcome").value("CREATED"))
                .andExpect(jsonPath("$.data.dealStatus").value("DRAFT"));

        Long dealId = jdbc.queryForObject("SELECT id FROM deal WHERE external_offer_key = ?", Long.class, "LISTING:" + ITEM_ID);
        assertEquals("DRAFT", jdbc.queryForObject("SELECT status FROM deal WHERE id = ?", String.class, dealId));
        assertEquals("AFFILIATE", jdbc.queryForObject("SELECT source_type FROM deal WHERE id = ?", String.class, dealId));
        assertEquals(null, jdbc.queryForObject("SELECT verified_at FROM deal WHERE id = ?", Object.class, dealId));
        assertEquals(history, historyCount());
        assertEquals(demoTitle, jdbc.queryForObject("SELECT title FROM product WHERE id = 2", String.class));
        assertEquals("DEMO", jdbc.queryForObject("SELECT source_type FROM deal WHERE id = 2", String.class));
        assertFalse(jdbc.queryForObject("SELECT product_code FROM product WHERE product_code LIKE 'TB-%'", String.class).startsWith("DEMO-"));

        mockMvc.perform(get("/api/v1/deals").param("keyword", "测试纸巾"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/imports").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + ITEM_ID + "\",\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productOutcome").value("ALREADY_EXISTS"))
                .andExpect(jsonPath("$.data.dealOutcome").value("ALREADY_EXISTS"))
                .andExpect(jsonPath("$.data.dealId").value(dealId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM deal WHERE external_offer_key = ?", Integer.class, "LISTING:" + ITEM_ID));
    }

    @Test
    void promotionFailureDoesNotStoreFakeLink() throws Exception {
        importItem();
        ScriptedTaobaoTransport.promotionBody = promotionJson("javascript:alert(1)");
        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/promotion-links").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + ITEM_ID + "\"}"))
                .andExpect(status().isBadGateway());
        assertEquals(null, jdbc.queryForObject("SELECT promotion_url FROM deal WHERE external_offer_key = ?", String.class, "LISTING:" + ITEM_ID));

        ScriptedTaobaoTransport.promotionBody = promotionJson("https://uland.taobao.com/coupon/edetail?e=abc");
        Long dealId = jdbc.queryForObject("SELECT id FROM deal WHERE external_offer_key = ?", Long.class, "LISTING:" + ITEM_ID);
        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/promotion-links").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + ITEM_ID + "\",\"dealId\":" + dealId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.persisted").value(true))
                .andExpect(jsonPath("$.data.miniProgramCompatible").value(false));
        assertEquals("https://uland.taobao.com/coupon/edetail?e=abc",
                jdbc.queryForObject("SELECT promotion_url FROM deal WHERE id = ?", String.class, dealId));
        assertEquals("taobao.tbk.privilege.get",
                jdbc.queryForObject("SELECT promotion_source FROM deal WHERE id = ?", String.class, dealId));
        assertEquals("DRAFT", jdbc.queryForObject("SELECT status FROM deal WHERE id = ?", String.class, dealId));
    }

    @Test
    void platformErrorIsNotPresentedAsSuccessAndTimeoutCanRetry() throws Exception {
        ScriptedTaobaoTransport.searchBody = """
                {"error_response":{"code":50,"msg":"Remote service error","sub_code":"isv.invalid-parameter","sub_msg":"非法参数 %s","request_id":"err-9"}}
                """.formatted(SECRET);
        MvcResult failed = mockMvc.perform(get("/api/admin/v1/affiliate/taobao/items").session(session).param("keyword", "纸巾"))
                .andExpect(status().isBadGateway())
                .andReturn();
        assertFalse(failed.getResponse().getContentAsString().contains(SECRET));
        assertEquals(0, jdbc.queryForObject("SELECT retryable FROM job_run WHERE job_name = 'TAOBAO_SEARCH' ORDER BY id DESC LIMIT 1", Integer.class));

        ScriptedTaobaoTransport.failure = new TaobaoTransportException("淘宝联盟请求超时", true, false);
        mockMvc.perform(get("/api/admin/v1/affiliate/taobao/items").session(session).param("keyword", "纸巾"))
                .andExpect(status().isServiceUnavailable());
        Long jobId = jdbc.queryForObject("SELECT id FROM job_run WHERE job_name = 'TAOBAO_SEARCH' AND retryable = 1 ORDER BY id DESC LIMIT 1", Long.class);
        ScriptedTaobaoTransport.failure = null;
        ScriptedTaobaoTransport.searchBody = searchJson();
        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/jobs/" + jobId + "/retry").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void missingCategoryDoesNotPublishAndDisabledSwitchIsClear() throws Exception {
        int products = jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class);
        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/imports").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + ITEM_ID + "\",\"categoryId\":999}"))
                .andExpect(status().isBadRequest());
        assertEquals(products, jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class));

        taobaoProperties.setEnabled(false);
        try {
            mockMvc.perform(get("/api/admin/v1/affiliate/taobao/items").session(session).param("keyword", "纸巾"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("淘宝联盟未启用"));
        } finally {
            taobaoProperties.setEnabled(true);
        }
    }

    @Test
    void concurrentImportCreatesOneProduct() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            futures.add(pool.submit(() -> {
                MvcResult result = mockMvc.perform(post("/api/admin/v1/affiliate/taobao/imports").session(session).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"itemId\":\"" + ITEM_ID + "\",\"categoryId\":1}"))
                        .andReturn();
                return result.getResponse().getStatus();
            }));
        }
        for (Future<Integer> future : futures) {
            assertEquals(200, future.get());
        }
        pool.shutdown();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM product_external_ref WHERE external_item_id = ?", Integer.class, ITEM_ID));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM deal WHERE external_offer_key = ?", Integer.class, "LISTING:" + ITEM_ID));
    }

    private void importItem() throws Exception {
        mockMvc.perform(post("/api/admin/v1/affiliate/taobao/imports").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + ITEM_ID + "\",\"categoryId\":1}"))
                .andExpect(status().isOk());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private int historyCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM price_history", Integer.class);
    }

    private static String searchJson() {
        return """
                {"tbk_dg_material_optional_response":{"total_results":1,"request_id":"search-1","result_list":{"map_data":[{
                  "item_id":"%s","title":"测试纸巾","pict_url":"https://img.alicdn.com/bao/uploaded/a.jpg","shop_title":"测试店",
                  "reserve_price":"12.00","zk_final_price":"9.90","coupon_id":"coupon-1","coupon_info":"满10减1",
                  "item_url":"https://item.taobao.com/item.htm?id=%s","url":"s.click.xxx"
                }]}}}
                """.formatted(ITEM_ID, ITEM_ID);
    }

    private static String detailJson(String itemId, String price) {
        return """
                {"tbk_item_info_get_response":{"request_id":"detail-1","results":{"n_tbk_item":[{
                  "num_iid":"%s","title":"测试纸巾","pict_url":"https://img.alicdn.com/bao/uploaded/a.jpg",
                  "reserve_price":"12.00","zk_final_price":"%s","item_url":"https://item.taobao.com/item.htm?id=%s","nick":"测试店"
                }]}}}
                """.formatted(itemId, price, itemId);
    }

    private static String promotionJson(String url) {
        return """
                {"tbk_privilege_get_response":{"request_id":"link-1","result":{"data":{"item_id":"%s","coupon_click_url":"%s","coupon_info":"满10减1"}}}}
                """.formatted(ITEM_ID, url);
    }

    static class FakeTransportConfig {
        @Bean
        @Primary
        TaobaoTransport taobaoTransport() {
            return new ScriptedTaobaoTransport();
        }
    }
}
