package com.greatprice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分类、好价接口的端到端测试：Controller → Service → Mapper XML → H2（加载 sql/schema.sql + db/test-data.sql）。
 * 测试数据中公开可见的好价只有 id 1、2、3。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DealApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions getDeals(String... params) throws Exception {
        var request = get("/api/v1/deals");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    @Test
    void categoriesReturnOnlyEnabledOrderedBySortOrder() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].code", contains("food", "digital")))
                .andExpect(jsonPath("$.data[0].name").value("食品饮料"))
                .andExpect(jsonPath("$.data[0].icon").value(nullValue()));
    }

    @Test
    void listReturnsOnlyPublicDealsByPublishedTimeDesc() throws Exception {
        getDeals()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                // 排除：商品下架(4)、商品逻辑删除(5)、草稿(6)、好价下架(7)、已过期(8)、未开始(9)、待审核(10)、定时发布未到(11)
                .andExpect(jsonPath("$.data.records[*].id", contains(1, 2, 3)));
    }

    @Test
    void listMapsCardFieldsAndPricesAsStrings() throws Exception {
        getDeals()
                .andExpect(jsonPath("$.data.records[0].title").value("降噪耳机 Pro"))
                .andExpect(jsonPath("$.data.records[0].brand").value("声途"))
                .andExpect(jsonPath("$.data.records[0].categoryId").value(1))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("数码家电"))
                .andExpect(jsonPath("$.data.records[0].currentPrice").value("299.00"))
                .andExpect(jsonPath("$.data.records[0].originalPrice").value("399.00"))
                .andExpect(jsonPath("$.data.records[0].currency").value("CNY"))
                .andExpect(jsonPath("$.data.records[0].sourceType").value("EDITOR"))
                .andExpect(jsonPath("$.data.records[0].verifiedAt").value("2026-01-03T10:00:00+08:00"))
                .andExpect(jsonPath("$.data.records[0].publishedAt").value("2026-01-03T09:00:00+08:00"))
                .andExpect(jsonPath("$.data.records[1].currentPrice").value("49.90"))
                .andExpect(jsonPath("$.data.records[1].originalPrice").value(nullValue()))
                .andExpect(jsonPath("$.data.records[1].verifiedAt").value(nullValue()))
                // 列表不返回链接
                .andExpect(jsonPath("$.data.records[0].purchaseUrl").doesNotExist());
    }

    @Test
    void listSortsByPriceWithWhitelist() throws Exception {
        getDeals("sortBy", "PRICE_ASC").andExpect(jsonPath("$.data.records[*].id", contains(2, 3, 1)));
        getDeals("sortBy", "PRICE_DESC").andExpect(jsonPath("$.data.records[*].id", contains(1, 3, 2)));
        getDeals("sortBy", "price; DROP TABLE deal")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("参数格式错误: sortBy"));
    }

    @Test
    void listFiltersByCategoryAndFeatured() throws Exception {
        getDeals("categoryId", "1").andExpect(jsonPath("$.data.records[*].id", contains(1, 3)));
        getDeals("categoryId", "2").andExpect(jsonPath("$.data.records[*].id", contains(2)));
        getDeals("categoryId", "999").andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records", empty()));
        getDeals("featured", "true").andExpect(jsonPath("$.data.records[*].id", contains(1)));
    }

    @Test
    void keywordMatchesTitleBrandAndCategoryName() throws Exception {
        getDeals("keyword", "耳机").andExpect(jsonPath("$.data.records[*].id", contains(1)));
        getDeals("keyword", "晨野").andExpect(jsonPath("$.data.records[*].id", contains(2)));
        getDeals("keyword", "数码").andExpect(jsonPath("$.data.records[*].id", contains(1, 3)));
        getDeals("keyword", "  pro  ").andExpect(jsonPath("$.data.records[*].id", contains(1)));
        getDeals("keyword", "不存在的商品").andExpect(jsonPath("$.data.total").value(0));
        getDeals("keyword", "已下架商品").andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void keywordHandlesBlankSpecialCharsAndLength() throws Exception {
        getDeals("keyword", "   ").andExpect(jsonPath("$.data.total").value(3));
        // LIKE 通配符按字面匹配，只有标题中包含 % 或 _ 的商品 3 命中
        getDeals("keyword", "%").andExpect(jsonPath("$.data.records[*].id", contains(3)));
        getDeals("keyword", "_").andExpect(jsonPath("$.data.records[*].id", contains(3)));
        getDeals("keyword", "!").andExpect(jsonPath("$.data.total").value(0));
        getDeals("keyword", "' OR '1'='1").andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        getDeals("keyword", "a".repeat(51))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("keyword: 不能超过 50 个字符"));
    }

    @Test
    void paginationIsDoneInDatabaseAndValidatesBounds() throws Exception {
        getDeals("page", "1", "size", "2")
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records[*].id", contains(1, 2)));
        getDeals("page", "2", "size", "2").andExpect(jsonPath("$.data.records[*].id", contains(3)));
        getDeals("page", "3", "size", "2")
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records", empty()));
        getDeals("size", "50").andExpect(status().isOk());

        getDeals("page", "0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("page: 不能小于 1"));
        getDeals("size", "0").andExpect(status().isBadRequest());
        getDeals("size", "51").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("size: 不能超过 50"));
        getDeals("page", "abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("参数格式错误: page"));
        getDeals("categoryId", "-1").andExpect(status().isBadRequest());
    }

    @Test
    void detailReturnsProductDealAndSafeLinks() throws Exception {
        mockMvc.perform(get("/api/v1/deals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.title").value("降噪耳机 Pro"))
                .andExpect(jsonPath("$.data.categoryName").value("数码家电"))
                .andExpect(jsonPath("$.data.specification").value("颜色：黑色"))
                .andExpect(jsonPath("$.data.description").value("商品描述"))
                .andExpect(jsonPath("$.data.currentPrice").value("299.00"))
                .andExpect(jsonPath("$.data.originalPrice").value("399.00"))
                .andExpect(jsonPath("$.data.dealDescription").value("券后价"))
                .andExpect(jsonPath("$.data.sourceName").value("测试商城"))
                .andExpect(jsonPath("$.data.startsAt").value(nullValue()))
                .andExpect(jsonPath("$.data.expiresAt").value("2999-12-31T23:59:59+08:00"))
                .andExpect(jsonPath("$.data.verifiedAt").value("2026-01-03T10:00:00+08:00"))
                .andExpect(jsonPath("$.data.purchaseUrl").value("https://item.example.com/1"))
                // 非 https 链接不返回
                .andExpect(jsonPath("$.data.promotionUrl").value(nullValue()));

        mockMvc.perform(get("/api/v1/deals/2"))
                .andExpect(jsonPath("$.data.sourceType").value("DEMO"))
                .andExpect(jsonPath("$.data.purchaseUrl").value(nullValue()));
    }

    @Test
    void detailOfMissingOrDraftDealReturnsNotFound() throws Exception {
        for (String id : new String[]{"999", "6", "10"}) {
            mockMvc.perform(get("/api/v1/deals/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(40400))
                    .andExpect(jsonPath("$.message").value("好价不存在"))
                    .andExpect(jsonPath("$.data").value(nullValue()));
        }
    }

    @Test
    void detailOfOfflineExpiredOrInactiveProductReturnsUnavailable() throws Exception {
        for (String id : new String[]{"4", "5", "7", "8"}) {
            mockMvc.perform(get("/api/v1/deals/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(40401))
                    .andExpect(jsonPath("$.message").value("好价已下架或已过期"));
        }
    }

    @Test
    void detailRejectsInvalidId() throws Exception {
        mockMvc.perform(get("/api/v1/deals/0")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(get("/api/v1/deals/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }
}
