package com.greatprice.controller;

import com.greatprice.common.exception.BizException;
import com.greatprice.security.AdminUserDetails;
import com.greatprice.security.AdminUserDetailsService;
import com.greatprice.security.LoginAttemptGuard;
import com.greatprice.service.DealAdminService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台接口测试，使用独立的 H2 库，避免改动公开接口测试依赖的种子数据可见性。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiIntegrationTest {

    private static final String ADMIN_PASSWORD = "Admin#Test12345";
    private static final String OPERATOR_PASSWORD = "Operator#Test12345";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:admin_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
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
    private DealAdminService dealAdminService;
    @Autowired
    private AdminUserDetailsService userDetailsService;

    private MockHttpSession adminSession;
    private MockHttpSession admin2Session;
    private MockHttpSession operatorSession;
    private long adminId;

    @BeforeEach
    void setUpUsers() throws Exception {
        loginAttemptGuard.reset();
        jdbc.update("DELETE FROM admin_user");
        insertUser("admin", ADMIN_PASSWORD, "管理员", "ADMIN");
        insertUser("admin2", ADMIN_PASSWORD, "管理员二", "ADMIN");
        insertUser("operator", OPERATOR_PASSWORD, "运营", "OPERATOR");
        adminId = jdbc.queryForObject("SELECT id FROM admin_user WHERE username = 'admin'", Long.class);
        adminSession = login("admin", ADMIN_PASSWORD);
        admin2Session = login("admin2", ADMIN_PASSWORD);
        operatorSession = login("operator", OPERATOR_PASSWORD);
    }

    @Test
    void anonymousCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/v1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
        mockMvc.perform(get("/api/admin/v1/deals"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/v1/deals").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicDealsStayOpenWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/v1/deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void loginFailureLockAndLogout() throws Exception {
        mockMvc.perform(post("/api/admin/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        insertUser("lockuser", ADMIN_PASSWORD, "锁定", "OPERATOR");
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/admin/v1/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("lockuser", "wrong-password")))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        }
        mockMvc.perform(post("/api/admin/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("lockuser", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登录失败次数过多，请 15 分钟后再试"));
        mockMvc.perform(post("/api/admin/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("lockuser", ADMIN_PASSWORD)))
                .andExpect(jsonPath("$.message").value("登录失败次数过多，请 15 分钟后再试"));

        mockMvc.perform(post("/api/admin/v1/auth/logout").session(adminSession).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/v1/auth/me").session(adminSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void operatorCannotReviewPublishOrManageUsers() throws Exception {
        long productId = createProduct(operatorSession, code("OP"));
        long dealId = createDeal(operatorSession, productId, "10.00", "EDITOR", "https://item.example.com/op");
        int version = submit(operatorSession, dealId);

        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(version)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/offline").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"下架\",\"version\":" + version + "}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/v1/audit-logs").session(operatorSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/v1/users").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hack\",\"password\":\"long-password\",\"displayName\":\"x\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertEquals("PENDING_REVIEW", statusOf(dealId));
    }

    @Test
    void cannotApproveOwnSubmissionAndRoleCannotBeForged() throws Exception {
        long productId = createProduct(adminSession, code("SELF"));
        String body = dealBody(productId, "12.00", "EDITOR", "https://item.example.com/self");
        MvcResult created = mockMvc.perform(post("/api/admin/v1/deals").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("}", ",\"status\":\"PUBLISHED\",\"submittedBy\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long dealId = idOf(created);
        int version = submit(adminSession, dealId);

        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(version)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("不能审核自己提交的好价"));
        assertEquals("PENDING_REVIEW", statusOf(dealId));

        mockMvc.perform(get("/api/admin/v1/auth/me").session(operatorSession))
                .andExpect(jsonPath("$.data.role").value("OPERATOR"));
    }

    @Test
    void reviewApproveRejectOfflineAndPublicVisibility() throws Exception {
        long productId = createProduct(operatorSession, code("FLOW"));
        long dealId = createDeal(operatorSession, productId, "19.90", "EDITOR", "https://item.example.com/flow");
        assertEquals(0, historyCount());

        mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(adminSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        assertEquals(0, historyCount());

        int pendingVersion = submit(operatorSession, dealId);
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/reject").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + pendingVersion + "}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/reject").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"价格与页面不一致\",\"version\":" + pendingVersion + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.rejectReason").value("价格与页面不一致"));

        MvcResult rejected = mockMvc.perform(get("/api/admin/v1/deals/" + dealId + "/reviews").session(operatorSession))
                .andExpect(jsonPath("$.data[0].action").value("REJECT"))
                .andExpect(jsonPath("$.data[0].reviewerName").value("管理员"))
                .andReturn();
        assertTrue(rejected.getResponse().getContentAsString().contains("价格与页面不一致"));

        int version = number(mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(operatorSession))
                .andReturn(), "$.data.version");
        int submitted = submit(operatorSession, dealId, version);
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(submitted)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPrice").value("19.90"))
                .andExpect(jsonPath("$.data.purchaseUrl").value("https://item.example.com/flow"));

        int publishedVersion = number(mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(adminSession)).andReturn(), "$.data.version");
        mockMvc.perform(put("/api/admin/v1/deals/" + dealId).session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, "18.00", "EDITOR", "https://item.example.com/flow2", publishedVersion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("已发布好价不能直接修改，请提交变更审核"));

        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/revise").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, "18.00", "EDITOR", "https://item.example.com/flow2", publishedVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.currentPrice").value("18.00"))
                .andExpect(jsonPath("$.data.verifiedAt").value(nullValue()));
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));

        int revised = number(mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(admin2Session)).andReturn(), "$.data.version");
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(admin2Session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(revised)))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        int online = number(mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(adminSession)).andReturn(), "$.data.version");
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/offline").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"活动结束\",\"version\":" + online + "}"))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.offlineReason").value("活动结束"));
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        int offlineVersion = number(mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(operatorSession)).andReturn(), "$.data.version");
        mockMvc.perform(put("/api/admin/v1/deals/" + dealId).session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, "18.00", "EDITOR", "https://item.example.com/flow2", offlineVersion)))
                .andExpect(jsonPath("$.message").value("已下架或已过期的好价不能直接修改或恢复发布"));
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/reopen").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(offlineVersion)))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
        assertNotEquals("PUBLISHED", statusOf(dealId));

        mockMvc.perform(get("/api/admin/v1/audit-logs").session(adminSession).param("action", "DEAL_APPROVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(not(0)))
                .andExpect(jsonPath("$.data.records[0].action").value("DEAL_APPROVE"));
    }

    @Test
    void validationRejectsBadPriceTimeAndLink() throws Exception {
        long productId = createProduct(adminSession, code("BAD"));
        mockMvc.perform(post("/api/admin/v1/deals").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, "-1", "EDITOR", "https://item.example.com/x")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/v1/deals").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, "1.239", "EDITOR", "https://item.example.com/x")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/v1/deals").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, "1.00", "EDITOR", "javascript:alert(1)")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("购买链接只允许 http 或 https"));
        String ranged = dealBody(productId, "1.00", "EDITOR", "https://item.example.com/x")
                .replace("}", ",\"startsAt\":\"2026-02-02T00:00:00+08:00\",\"expiresAt\":\"2026-01-01T00:00:00+08:00\"}");
        mockMvc.perform(post("/api/admin/v1/deals").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ranged))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("结束时间必须晚于开始时间"));

        long zero = createDeal(adminSession, productId, "0.00", "EDITOR", null);
        mockMvc.perform(get("/api/admin/v1/deals/" + zero).session(adminSession))
                .andExpect(jsonPath("$.data.currentPrice").value("0.00"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void expiredDealIsNotPublic() throws Exception {
        long productId = createProduct(operatorSession, code("EXP"));
        String body = dealBody(productId, "8.00", "EDITOR", "https://item.example.com/exp")
                .replace("}", ",\"expiresAt\":\"2000-01-01T00:00:00+08:00\"}");
        MvcResult created = mockMvc.perform(post("/api/admin/v1/deals").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        long dealId = idOf(created);
        int version = submit(operatorSession, dealId);
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(version)))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void httpLinkIsStoredButNotExposedPublicly() throws Exception {
        long productId = createProduct(operatorSession, code("HTTP"));
        long dealId = createDeal(operatorSession, productId, "6.00", "EDITOR", "http://item.example.com/http");
        int version = submit(operatorSession, dealId);
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(version)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(adminSession))
                .andExpect(jsonPath("$.data.purchaseUrl").value("http://item.example.com/http"));
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(jsonPath("$.data.purchaseUrl").value(nullValue()));
    }

    @Test
    void productRulesAndDisablingHidesPublicDeal() throws Exception {
        mockMvc.perform(post("/api/admin/v1/products").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("DUP-1", 1)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/v1/products").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("DUP-1", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品编码已存在"));
        mockMvc.perform(post("/api/admin/v1/products").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(code("OLD"), 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("分类不存在或已停用"));

        long productId = createProduct(operatorSession, code("HIDE"));
        long dealId = createDeal(operatorSession, productId, "15.00", "EDITOR", "https://item.example.com/hide");
        int version = submit(operatorSession, dealId);
        mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/approve").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(version)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/deals/" + dealId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/v1/products/" + productId + "/disable").session(operatorSession).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(post("/api/admin/v1/products/" + productId + "/enable").session(adminSession).with(csrf()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/deals/" + dealId)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/v1/products/" + productId).session(adminSession).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/deals/" + dealId))
                .andExpect(jsonPath("$.code").value(40401));
        mockMvc.perform(get("/api/admin/v1/products").session(adminSession).param("keyword", "HIDE"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void priceVerificationWritesHistoryAndIgnoresClientIdentity() throws Exception {
        int before = historyCount();
        long productId = createProduct(operatorSession, code("VER"));
        long dealId = createDeal(operatorSession, productId, "21.00", "EDITOR", "https://item.example.com/ver");
        assertEquals(before, historyCount());

        String forged = """
                {"productId":%d,"dealId":%d,"price":"21.50","sourceType":"EDITOR","sourceName":"页面价","note":"已核对","verifiedBy":999,"recordedAt":"2000-01-01T00:00:00+08:00"}
                """.formatted(productId, dealId);
        mockMvc.perform(post("/api/admin/v1/price-history").session(operatorSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(forged))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value("21.50"))
                .andExpect(jsonPath("$.data.verificationStatus").value("MANUAL_VERIFIED"))
                .andExpect(jsonPath("$.data.verifiedBy").value(jdbc.queryForObject(
                        "SELECT id FROM admin_user WHERE username = 'operator'", Long.class)))
                .andExpect(jsonPath("$.data.verifierName").value("运营"))
                .andExpect(jsonPath("$.data.recordedAt").value(not("2000-01-01T00:00:00+08:00")));
        assertEquals(before + 1, historyCount());

        mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(adminSession)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/deals/1")).andExpect(status().isOk());
        assertEquals(before + 1, historyCount());

        mockMvc.perform(post("/api/admin/v1/price-history").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"dealId":2,"price":"1.00","sourceType":"EDITOR","sourceName":"伪造"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("演示数据不能写入真实价格历史"));
        assertEquals(before + 1, historyCount());

        long demoProduct = createProduct(adminSession, "DEMO-" + System.nanoTime());
        mockMvc.perform(post("/api/admin/v1/price-history").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"price":"1.00","sourceType":"EDITOR","sourceName":"伪造"}
                                """.formatted(demoProduct)))
                .andExpect(jsonPath("$.message").value("演示数据不能写入真实价格历史"));
        mockMvc.perform(post("/api/admin/v1/price-history").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":%d,"price":"1.00","sourceType":"DEMO","sourceName":"演示"}
                                """.formatted(productId)))
                .andExpect(jsonPath("$.message").value("演示数据不能写入真实价格历史"));
        assertEquals(before + 1, historyCount());

        mockMvc.perform(put("/api/admin/v1/price-history/1").session(adminSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":\"1.00\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardMatchesDatabase() throws Exception {
        Long products = jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE deleted = 0", Long.class);
        mockMvc.perform(get("/api/admin/v1/dashboard").session(operatorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCount").value(products))
                .andExpect(jsonPath("$.data.pendingCount").value(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM deal WHERE status = 'PENDING_REVIEW'", Long.class)))
                .andExpect(jsonPath("$.data.offlineCount").value(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM deal WHERE status = 'OFFLINE'", Long.class)))
                .andExpect(jsonPath("$.data.recentDeals").isArray());
    }

    @Test
    void cannotDisableTheOnlyAdmin() throws Exception {
        long admin2Id = jdbc.queryForObject("SELECT id FROM admin_user WHERE username = 'admin2'", Long.class);
        mockMvc.perform(post("/api/admin/v1/users/" + admin2Id + "/disable").session(adminSession).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
        mockMvc.perform(post("/api/admin/v1/users/" + adminId + "/disable").session(adminSession).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能停用唯一的管理员"));
    }

    @Test
    void concurrentApproveDoesNotDoublePublish() throws Exception {
        long productId = createProduct(operatorSession, code("RACE"));
        long dealId = createDeal(operatorSession, productId, "3.00", "EDITOR", "https://item.example.com/race");
        int version = submit(operatorSession, dealId);
        AdminUserDetails admin = (AdminUserDetails) userDetailsService.loadUserByUsername("admin");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        Runnable task = () -> {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));
            SecurityContextHolder.setContext(context);
            try {
                start.await();
                dealAdminService.approve(dealId, version);
                success.incrementAndGet();
            } catch (BizException ex) {
                rejected.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        Future<?> first = pool.submit(task);
        Future<?> second = pool.submit(task);
        start.countDown();
        first.get();
        second.get();
        pool.shutdown();
        assertEquals(1, success.get());
        assertEquals(1, rejected.get());
        assertEquals("PUBLISHED", statusOf(dealId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM deal_review WHERE deal_id = ? AND action = 'APPROVE'", Integer.class, dealId));
    }

    private void insertUser(String username, String password, String displayName, String role) {
        jdbc.update("""
                INSERT INTO admin_user (username, password_hash, display_name, role, status, failed_login_count)
                VALUES (?, ?, ?, ?, 'ACTIVE', 0)
                """, username, passwordEncoder.encode(password), displayName, role);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long createProduct(MockHttpSession session, String productCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/v1/products").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(productCode, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        return idOf(result);
    }

    private long createDeal(MockHttpSession session, long productId, String price, String sourceType, String purchaseUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/v1/deals").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dealBody(productId, price, sourceType, purchaseUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        return idOf(result);
    }

    private int submit(MockHttpSession session, long dealId) throws Exception {
        int version = number(mockMvc.perform(get("/api/admin/v1/deals/" + dealId).session(session)).andReturn(), "$.data.version");
        return submit(session, dealId, version);
    }

    private int submit(MockHttpSession session, long dealId, int version) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/v1/deals/" + dealId + "/submit").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(versionBody(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();
        return number(result, "$.data.version");
    }

    private static String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String versionBody(int version) {
        return "{\"version\":" + version + "}";
    }

    private static String productBody(String productCode, long categoryId) {
        return """
                {"productCode":"%s","title":"测试商品","brand":"声途","categoryId":%d,"imageUrl":"https://img.example.com/a.png","specification":"黑色","description":"描述"}
                """.formatted(productCode, categoryId);
    }

    private static String dealBody(long productId, String price, String sourceType, String purchaseUrl) {
        return dealBody(productId, price, sourceType, purchaseUrl, null);
    }

    private static String dealBody(long productId, String price, String sourceType, String purchaseUrl, Integer version) {
        String url = purchaseUrl == null ? "null" : "\"" + purchaseUrl + "\"";
        String versionField = version == null ? "" : ",\"version\":" + version;
        return """
                {"productId":%d,"currentPrice":"%s","originalPrice":"30.00","dealDescription":"测试优惠","sourceType":"%s","sourceName":"测试店","purchaseUrl":%s%s}
                """.formatted(productId, price, sourceType, url, versionField);
    }

    private static long idOf(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private static int number(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).intValue();
    }

    private String statusOf(long dealId) {
        return jdbc.queryForObject("SELECT status FROM deal WHERE id = ?", String.class, dealId);
    }

    private int historyCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM price_history", Integer.class);
    }

    private static String code(String prefix) {
        return prefix + System.nanoTime();
    }
}
