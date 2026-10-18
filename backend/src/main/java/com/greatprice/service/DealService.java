package com.greatprice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.api.PageResult;
import com.greatprice.common.exception.BizException;
import com.greatprice.dto.DealDetailResponse;
import com.greatprice.dto.DealQueryRequest;
import com.greatprice.dto.DealSummaryResponse;
import com.greatprice.entity.Deal;
import com.greatprice.entity.DealRow;
import com.greatprice.mapper.DealMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * C 端好价查询。公开可见规则在 DealMapper.xml 的 publicCondition 中统一定义。
 */
@Service
public class DealService {

    /** 数据库 DATETIME 按北京时间存储，与 JDBC 连接参数 serverTimezone=Asia/Shanghai 一致。 */
    private static final ZoneId DB_ZONE = ZoneId.of("Asia/Shanghai");

    /** 曾经公开过的状态；这些好价不可访问时提示“已下架或已过期”，草稿和待审核则一律视为不存在。 */
    private static final Set<String> ONCE_PUBLIC_STATUSES = Set.of(Deal.STATUS_PUBLISHED, Deal.STATUS_OFFLINE, Deal.STATUS_EXPIRED);

    private final DealMapper dealMapper;

    public DealService(DealMapper dealMapper) {
        this.dealMapper = dealMapper;
    }

    public PageResult<DealSummaryResponse> listPublic(DealQueryRequest query) {
        IPage<DealRow> page = dealMapper.selectPublicPage(
                Page.of(query.page(), query.size()),
                query.categoryId(),
                escapeLike(query.keyword()),
                query.featured(),
                query.sortBy().name(),
                now());
        return PageResult.of(page, this::toSummary);
    }

    public DealDetailResponse getPublicDetail(Long id) {
        DealRow row = dealMapper.selectPublicById(id, now());
        if (row != null) {
            return toDetail(row);
        }
        Deal deal = dealMapper.selectById(id);
        if (deal != null && ONCE_PUBLIC_STATUSES.contains(deal.getStatus())) {
            throw new BizException(ErrorCode.DEAL_UNAVAILABLE);
        }
        throw new BizException(ErrorCode.NOT_FOUND, "好价不存在");
    }

    /** 转义 LIKE 通配符，转义符 ! 与 DealMapper.xml 中的 ESCAPE '!' 对应。 */
    static String escapeLike(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /** 只放行可解析的 https 链接，避免把脏数据或 javascript: 等协议交给前端跳转。 */
    static String safeHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url.strip());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null ? uri.toString() : null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(DB_ZONE);
    }

    private static OffsetDateTime toOffset(LocalDateTime time) {
        return time == null ? null : time.atZone(DB_ZONE).toOffsetDateTime();
    }

    private DealSummaryResponse toSummary(DealRow row) {
        return new DealSummaryResponse(
                row.getId(),
                row.getTitle(),
                row.getBrand(),
                row.getImageUrl(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getCurrentPrice(),
                row.getOriginalPrice(),
                row.getCurrency(),
                row.getDealDescription(),
                row.getSourceType(),
                row.getSourceName(),
                toOffset(row.getExpiresAt()),
                toOffset(row.getVerifiedAt()),
                toOffset(row.getPublishedAt()));
    }

    private DealDetailResponse toDetail(DealRow row) {
        return new DealDetailResponse(
                row.getId(),
                row.getProductId(),
                row.getTitle(),
                row.getBrand(),
                row.getImageUrl(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getSpecification(),
                row.getDescription(),
                row.getCurrentPrice(),
                row.getOriginalPrice(),
                row.getCurrency(),
                row.getDealDescription(),
                row.getSourceType(),
                row.getSourceName(),
                toOffset(row.getStartsAt()),
                toOffset(row.getExpiresAt()),
                toOffset(row.getVerifiedAt()),
                toOffset(row.getPublishedAt()),
                safeHttpsUrl(row.getPurchaseUrl()),
                safeHttpsUrl(row.getPromotionUrl()));
    }
}
