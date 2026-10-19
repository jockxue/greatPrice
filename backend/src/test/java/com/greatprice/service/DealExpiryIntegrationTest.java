package com.greatprice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealExpiryIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 18, 12, 0);

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:expiry_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1");
    }

    @Autowired
    private DealExpiryService dealExpiryService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void expiresOnlyPublishedDealsWhoseEndTimeHasArrived() throws Exception {
        long past = insert("PUBLISHED", "2026-10-18 11:00:00", "9.00");
        long exact = insert("PUBLISHED", "2026-10-18 12:00:00", "9.00");
        long future = insert("PUBLISHED", "2026-10-19 12:00:00", "9.00");
        long openEnded = insert("PUBLISHED", null, "9.00");
        long draft = insert("DRAFT", "2026-10-18 11:00:00", "9.00");
        long pending = insert("PENDING_REVIEW", "2026-10-18 11:00:00", "9.00");
        long offline = insert("OFFLINE", "2026-10-18 11:00:00", "9.00");
        long already = insert("EXPIRED", "2026-10-18 11:00:00", "9.00");

        mockMvc.perform(get("/api/v1/deals/" + past))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
        assertEquals("PUBLISHED", statusOf(past));

        int dueBefore = jdbc.queryForObject("""
                SELECT COUNT(*) FROM deal
                WHERE status = 'PUBLISHED' AND expires_at IS NOT NULL AND expires_at <= ?
                """, Integer.class, NOW);
        assertEquals(dueBefore, dealExpiryService.expireAll(NOW, 200));

        assertEquals("EXPIRED", statusOf(past));
        assertEquals("EXPIRED", statusOf(exact));
        assertEquals("PUBLISHED", statusOf(future));
        assertEquals("PUBLISHED", statusOf(openEnded));
        assertEquals("DRAFT", statusOf(draft));
        assertEquals("PENDING_REVIEW", statusOf(pending));
        assertEquals("OFFLINE", statusOf(offline));
        assertEquals("EXPIRED", statusOf(already));
        assertEquals(0, priceOf(past).compareTo(new BigDecimal("9.00")));
        assertNull(jdbc.queryForObject("SELECT verified_at FROM deal WHERE id = ?", Object.class, past));

        mockMvc.perform(get("/api/v1/deals/" + past)).andExpect(status().isNotFound());
        assertEquals(0, dealExpiryService.expireAll(NOW, 200));
        assertEquals(0, jdbc.queryForObject("SELECT version FROM deal WHERE id = ?", Integer.class, already));
    }

    @Test
    void batchesDoNotSkipDueDeals() {
        long[] ids = new long[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = insert("PUBLISHED", "2020-01-01 00:00:00", "3.00");
        }
        dealExpiryService.expireAll(NOW, 2);
        for (long id : ids) {
            assertEquals("EXPIRED", statusOf(id));
        }
    }

    @Test
    void inactiveProductStaysHidden() throws Exception {
        mockMvc.perform(get("/api/v1/deals/4"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    private long insert(String status, String expiresAt, String price) {
        jdbc.update("""
                INSERT INTO deal (product_id, current_price, deal_description, source_type, source_name, status, published_at, expires_at, version)
                VALUES (1, ?, '过期测试', 'EDITOR', '测试', ?, '2020-01-01 00:00:00', ?, 0)
                """, new BigDecimal(price), status, expiresAt);
        return jdbc.queryForObject("SELECT MAX(id) FROM deal", Long.class);
    }

    private String statusOf(long id) {
        return jdbc.queryForObject("SELECT status FROM deal WHERE id = ?", String.class, id);
    }

    private BigDecimal priceOf(long id) {
        return jdbc.queryForObject("SELECT current_price FROM deal WHERE id = ?", BigDecimal.class, id);
    }
}
