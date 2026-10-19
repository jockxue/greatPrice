package com.greatprice.integration.taobao;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;

import java.util.ArrayList;
import java.util.List;

/** 只读取官方响应里出现过的字段。缺失字段保持 null，不补演示值。 */
public final class TaobaoPayloads {

    public static final String SEARCH_METHOD = "taobao.tbk.dg.material.optional";
    public static final String DETAIL_METHOD = "taobao.tbk.item.info.get";
    public static final String PROMOTION_METHOD = "taobao.tbk.privilege.get";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TaobaoPayloads() {
    }

    public record Failure(String code, String subCode, String subMsg, String requestId) {
        public boolean retryable() {
            return TaobaoErrors.retryable(code, subCode);
        }

        public boolean emptyResult() {
            return TaobaoErrors.emptyResult(code, subCode);
        }
    }

    public record SearchHit(
            String itemId,
            String title,
            String imageUrl,
            String shopTitle,
            String reservePrice,
            String finalPrice,
            String salePrice,
            String couponId,
            String couponInfo,
            String couponStart,
            String couponEnd,
            String itemUrl,
            String clickUrl,
            String categoryName) {
    }

    public record SearchPage(String requestId, long total, List<SearchHit> hits, Failure failure) {
    }

    public record Detail(String requestId, SearchHit item, String nick, String catName, String catLeafName,
                         String volume, String userType, String playInfo, String presaleText,
                         String presaleStart, String presaleEnd, String kuadian, Failure failure) {
    }

    public record Promotion(String requestId, String itemId, String couponClickUrl, String itemUrl,
                            String ysylClickUrl, String couponInfo, String couponStart, String couponEnd,
                            Failure failure) {
    }

    public static JsonNode readTree(String body) {
        if (body == null || body.isBlank()) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "淘宝联盟响应为空");
        }
        try {
            return MAPPER.readTree(body);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "淘宝联盟响应格式异常");
        }
    }

    public static Failure failure(JsonNode root) {
        JsonNode error = root == null ? null : root.get("error_response");
        if (error == null || error.isNull()) {
            return null;
        }
        return new Failure(text(error, "code"), text(error, "sub_code"), text(error, "sub_msg"), text(error, "request_id"));
    }

    public static SearchPage search(JsonNode root) {
        Failure error = failure(root);
        if (error != null) {
            if (error.emptyResult()) {
                return new SearchPage(error.requestId(), 0, List.of(), null);
            }
            return new SearchPage(error.requestId(), 0, List.of(), error);
        }
        JsonNode response = root.path("tbk_dg_material_optional_response");
        if (response.isMissingNode() || response.isNull()) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "淘宝联盟响应格式异常");
        }
        List<SearchHit> hits = new ArrayList<>();
        for (JsonNode node : nodes(response.path("result_list").path("map_data"))) {
            hits.add(searchHit(node));
        }
        return new SearchPage(text(response, "request_id"), response.path("total_results").asLong(hits.size()), hits, null);
    }

    public static Detail detail(JsonNode root, String requestedItemId) {
        Failure error = failure(root);
        if (error != null) {
            return new Detail(error.requestId(), null, null, null, null, null, null, null, null, null, null, null, error);
        }
        JsonNode response = root.path("tbk_item_info_get_response");
        if (response.isMissingNode() || response.isNull()) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "淘宝联盟响应格式异常");
        }
        List<JsonNode> items = nodes(response.path("results").path("n_tbk_item"));
        JsonNode matched = null;
        for (JsonNode node : items) {
            String numIid = text(node, "num_iid");
            String inputId = text(node, "input_num_iid");
            if (requestedItemId.equals(numIid) || requestedItemId.equals(inputId)) {
                matched = node;
                break;
            }
        }
        if (matched == null && items.size() == 1) {
            matched = items.get(0);
        }
        if (matched == null) {
            return new Detail(text(response, "request_id"), null, null, null, null, null, null, null, null, null, null, null, null);
        }
        String itemId = first(text(matched, "num_iid"), text(matched, "input_num_iid"), requestedItemId);
        SearchHit hit = new SearchHit(
                itemId,
                text(matched, "title"),
                first(text(matched, "pict_url"), null),
                text(matched, "nick"),
                text(matched, "reserve_price"),
                text(matched, "zk_final_price"),
                text(matched, "sale_price"),
                null,
                null,
                null,
                null,
                text(matched, "item_url"),
                null,
                first(text(matched, "cat_leaf_name"), text(matched, "cat_name")));
        return new Detail(
                text(response, "request_id"),
                hit,
                text(matched, "nick"),
                text(matched, "cat_name"),
                text(matched, "cat_leaf_name"),
                text(matched, "volume"),
                text(matched, "user_type"),
                text(matched, "play_info"),
                text(matched, "presale_discount_fee_text"),
                text(matched, "presale_start_time"),
                text(matched, "presale_end_time"),
                text(matched, "kuadian_promotion_info"),
                null);
    }

    public static Promotion promotion(JsonNode root) {
        Failure error = failure(root);
        if (error != null) {
            return new Promotion(error.requestId(), null, null, null, null, null, null, null, error);
        }
        JsonNode response = root.path("tbk_privilege_get_response");
        JsonNode data = response.path("result").path("data");
        if (response.isMissingNode() || data.isMissingNode() || data.isNull()) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "淘宝联盟响应格式异常");
        }
        return new Promotion(
                text(response, "request_id"),
                text(data, "item_id"),
                text(data, "coupon_click_url"),
                text(data, "item_url"),
                text(data, "ysyl_click_url"),
                text(data, "coupon_info"),
                text(data, "coupon_start_time"),
                text(data, "coupon_end_time"),
                null);
    }

    private static SearchHit searchHit(JsonNode node) {
        return new SearchHit(
                first(text(node, "item_id"), text(node, "num_iid")),
                text(node, "title"),
                first(text(node, "pict_url"), text(node, "white_image")),
                first(text(node, "shop_title"), text(node, "nick")),
                text(node, "reserve_price"),
                text(node, "zk_final_price"),
                text(node, "sale_price"),
                text(node, "coupon_id"),
                text(node, "coupon_info"),
                text(node, "coupon_start_time"),
                text(node, "coupon_end_time"),
                text(node, "item_url"),
                first(text(node, "coupon_share_url"), text(node, "url")),
                first(text(node, "category_name"), text(node, "level_one_category_name")));
    }

    private static List<JsonNode> nodes(JsonNode node) {
        List<JsonNode> list = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return list;
        }
        if (node.isArray()) {
            node.forEach(list::add);
            return list;
        }
        if (node.isObject()) {
            list.add(node);
        }
        return list;
    }

    static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText();
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return value.strip();
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
