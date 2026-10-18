package com.greatprice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.api.PageResult;
import com.greatprice.common.exception.BizException;
import com.greatprice.common.web.DbTimes;
import com.greatprice.common.web.MoneyText;
import com.greatprice.common.web.WebUrls;
import com.greatprice.dto.admin.AdminDealQuery;
import com.greatprice.dto.admin.AdminDealResponse;
import com.greatprice.dto.admin.DealReviewResponse;
import com.greatprice.dto.admin.DealWriteRequest;
import com.greatprice.entity.Deal;
import com.greatprice.entity.DealReview;
import com.greatprice.entity.DealReviewRow;
import com.greatprice.entity.DealSource;
import com.greatprice.entity.AdminDealRow;
import com.greatprice.entity.Product;
import com.greatprice.mapper.DealMapper;
import com.greatprice.mapper.DealReviewMapper;
import com.greatprice.mapper.ProductMapper;
import com.greatprice.security.AdminActors;
import com.greatprice.security.AdminUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DealAdminService {

    private static final Set<String> EDITABLE = Set.of(Deal.STATUS_DRAFT, Deal.STATUS_PENDING_REVIEW);
    private static final Set<String> REOPENABLE = Set.of(Deal.STATUS_OFFLINE, Deal.STATUS_EXPIRED);

    private final DealMapper dealMapper;
    private final DealReviewMapper dealReviewMapper;
    private final ProductMapper productMapper;
    private final AuditService auditService;
    private final AdminActors adminActors;

    public DealAdminService(DealMapper dealMapper, DealReviewMapper dealReviewMapper, ProductMapper productMapper,
                            AuditService auditService, AdminActors adminActors) {
        this.dealMapper = dealMapper;
        this.dealReviewMapper = dealReviewMapper;
        this.productMapper = productMapper;
        this.auditService = auditService;
        this.adminActors = adminActors;
    }

    public PageResult<AdminDealResponse> page(AdminDealQuery query) {
        adminActors.current();
        if (query.status() != null && !Deal.STATUSES.contains(query.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "状态不合法");
        }
        if (query.sourceType() != null && !DealSource.ALL.contains(query.sourceType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "来源类型不合法");
        }
        Long dealId = numericId(query.keyword());
        IPage<AdminDealRow> page = dealMapper.selectAdminPage(
                Page.of(query.page(), query.size()),
                query.categoryId(),
                query.productId(),
                DealService.escapeLike(query.keyword()),
                dealId,
                query.status(),
                query.sourceType(),
                query.sortBy().name());
        return PageResult.of(page, this::toResponse);
    }

    public PageResult<AdminDealResponse> pending(AdminDealQuery query) {
        return page(new AdminDealQuery(query.page(), query.size(), query.categoryId(), query.productId(),
                query.keyword(), Deal.STATUS_PENDING_REVIEW, query.sourceType(), query.sortBy()));
    }

    public List<AdminDealResponse> recent() {
        adminActors.current();
        IPage<AdminDealRow> page = dealMapper.selectAdminPage(Page.of(1, 8), null, null, null, null, null, null, "CREATED_AT_DESC");
        return page.getRecords().stream().map(this::toResponse).toList();
    }

    public AdminDealResponse get(Long id) {
        adminActors.current();
        return toResponse(requireRow(id));
    }

    public List<DealReviewResponse> reviews(Long id) {
        adminActors.current();
        requireRow(id);
        return dealReviewMapper.selectByDealId(id).stream().map(this::toReview).toList();
    }

    @Transactional
    public AdminDealResponse create(DealWriteRequest request) {
        AdminUserDetails actor = adminActors.current();
        Deal deal = new Deal();
        fill(deal, request);
        deal.setStatus(Deal.STATUS_DRAFT);
        deal.setFeatured(Boolean.FALSE);
        deal.setVersion(0);
        dealMapper.insert(deal);
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_CREATE, "DEAL", deal.getId(),
                "productId=" + deal.getProductId());
        return toResponse(requireRow(deal.getId()));
    }

    @Transactional
    public AdminDealResponse update(Long id, DealWriteRequest request) {
        AdminUserDetails actor = adminActors.current();
        int version = requireVersion(request.version());
        Deal existing = requireDeal(id);
        if (Deal.STATUS_PUBLISHED.equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已发布好价不能直接修改，请提交变更审核");
        }
        if (REOPENABLE.contains(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "已下架或已过期的好价不能直接修改或恢复发布");
        }
        if (!EDITABLE.contains(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "当前状态不允许该操作");
        }
        fill(existing, request);
        existing.setVersion(version);
        ensure(dealMapper.updateEditable(existing), id, EDITABLE);
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_UPDATE, "DEAL", id, null);
        return toResponse(requireRow(id));
    }

    @Transactional
    public AdminDealResponse submit(Long id, int version) {
        AdminUserDetails actor = adminActors.current();
        Deal existing = requireDeal(id);
        if (!Deal.STATUS_DRAFT.equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有草稿可以提交审核");
        }
        ensure(dealMapper.submitReview(id, version, actor.getId()), id, Set.of(Deal.STATUS_DRAFT));
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_SUBMIT, "DEAL", id, null);
        return toResponse(requireRow(id));
    }

    @Transactional
    public AdminDealResponse approve(Long id, int version) {
        AdminUserDetails actor = adminActors.requireAdmin();
        Deal existing = requireDeal(id);
        if (actor.getId().equals(existing.getSubmittedBy())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能审核自己提交的好价");
        }
        if (!Deal.STATUS_PENDING_REVIEW.equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待审核好价可以通过");
        }
        LocalDateTime now = DbTimes.now();
        ensure(dealMapper.approve(id, version, actor.getId(), now), id, Set.of(Deal.STATUS_PENDING_REVIEW));
        insertReview(id, DealReview.APPROVE, actor.getId(), null);
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_APPROVE, "DEAL", id, null);
        return toResponse(requireRow(id));
    }

    @Transactional
    public AdminDealResponse reject(Long id, int version, String reason) {
        AdminUserDetails actor = adminActors.requireAdmin();
        Deal existing = requireDeal(id);
        if (actor.getId().equals(existing.getSubmittedBy())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能审核自己提交的好价");
        }
        if (!Deal.STATUS_PENDING_REVIEW.equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有待审核好价可以驳回");
        }
        String text = reason.strip();
        ensure(dealMapper.reject(id, version, actor.getId(), text, DbTimes.now()), id, Set.of(Deal.STATUS_PENDING_REVIEW));
        insertReview(id, DealReview.REJECT, actor.getId(), text);
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_REJECT, "DEAL", id, text);
        return toResponse(requireRow(id));
    }

    @Transactional
    public AdminDealResponse offline(Long id, int version, String reason) {
        AdminUserDetails actor = adminActors.requireAdmin();
        Deal existing = requireDeal(id);
        if (!Deal.STATUS_PUBLISHED.equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有已发布好价可以下架");
        }
        String text = reason.strip();
        ensure(dealMapper.offline(id, version, text), id, Set.of(Deal.STATUS_PUBLISHED));
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_OFFLINE, "DEAL", id, text);
        return toResponse(requireRow(id));
    }

    @Transactional
    public AdminDealResponse revise(Long id, DealWriteRequest request) {
        AdminUserDetails actor = adminActors.current();
        int version = requireVersion(request.version());
        Deal existing = requireDeal(id);
        if (!Deal.STATUS_PUBLISHED.equals(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有已发布好价可以提交变更审核");
        }
        Deal updated = new Deal();
        fill(updated, request);
        if (sameContent(existing, updated)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "内容没有变化，无需重新审核");
        }
        updated.setId(id);
        updated.setVersion(version);
        updated.setSubmittedBy(actor.getId());
        ensure(dealMapper.revisePublished(updated), id, Set.of(Deal.STATUS_PUBLISHED));
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_REVISE, "DEAL", id, null);
        return toResponse(requireRow(id));
    }

    @Transactional
    public AdminDealResponse reopen(Long id, int version) {
        AdminUserDetails actor = adminActors.current();
        Deal existing = requireDeal(id);
        if (!REOPENABLE.contains(existing.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "只有已下架或已过期的好价可以重新打开为草稿");
        }
        ensure(dealMapper.reopen(id, version), id, REOPENABLE);
        auditService.record(actor.getId(), actor.getUsername(), AuditService.DEAL_REOPEN, "DEAL", id, null);
        return toResponse(requireRow(id));
    }

    private void fill(Deal deal, DealWriteRequest request) {
        Product product = productMapper.selectById(request.productId());
        if (product == null || product.getDeleted() == null || product.getDeleted() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品不存在");
        }
        if (!DealSource.ALL.contains(request.sourceType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "来源类型不合法");
        }
        String currency = request.currency() == null || request.currency().isBlank() ? "CNY" : request.currency().strip();
        if (!"CNY".equals(currency)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "目前只支持 CNY");
        }
        LocalDateTime startsAt = DbTimes.toLocal(request.startsAt());
        LocalDateTime expiresAt = DbTimes.toLocal(request.expiresAt());
        if (startsAt != null && expiresAt != null && !startsAt.isBefore(expiresAt)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        deal.setProductId(product.getId());
        deal.setCurrentPrice(MoneyText.parse(request.currentPrice(), "当前价格", true));
        deal.setOriginalPrice(MoneyText.parse(request.originalPrice(), "参考价格", false));
        deal.setCurrency(currency);
        deal.setDealDescription(request.dealDescription().strip());
        deal.setSourceType(request.sourceType());
        deal.setSourceName(request.sourceName().strip());
        deal.setPurchaseUrl(WebUrls.normalize(request.purchaseUrl(), "购买链接", 1024));
        deal.setPromotionUrl(WebUrls.normalize(request.promotionUrl(), "推广链接", 1024));
        deal.setStartsAt(startsAt);
        deal.setExpiresAt(expiresAt);
    }

    private boolean sameContent(Deal current, Deal updated) {
        return Objects.equals(current.getProductId(), updated.getProductId())
                && sameMoney(current.getCurrentPrice(), updated.getCurrentPrice())
                && sameMoney(current.getOriginalPrice(), updated.getOriginalPrice())
                && Objects.equals(current.getCurrency(), updated.getCurrency())
                && Objects.equals(current.getDealDescription(), updated.getDealDescription())
                && Objects.equals(current.getSourceType(), updated.getSourceType())
                && Objects.equals(current.getSourceName(), updated.getSourceName())
                && Objects.equals(current.getPurchaseUrl(), updated.getPurchaseUrl())
                && Objects.equals(current.getPromotionUrl(), updated.getPromotionUrl())
                && Objects.equals(current.getStartsAt(), updated.getStartsAt())
                && Objects.equals(current.getExpiresAt(), updated.getExpiresAt());
    }

    private static boolean sameMoney(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.compareTo(right) == 0;
    }

    private void insertReview(Long dealId, String action, Long reviewerId, String reason) {
        DealReview review = new DealReview();
        review.setDealId(dealId);
        review.setAction(action);
        review.setReviewerId(reviewerId);
        review.setReason(reason);
        dealReviewMapper.insert(review);
    }

    private void ensure(int rows, Long id, Set<String> expectedStatuses) {
        if (rows == 1) {
            return;
        }
        Deal current = dealMapper.selectById(id);
        if (current == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "好价不存在");
        }
        if (expectedStatuses.contains(current.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT);
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "当前状态不允许该操作");
    }

    private Deal requireDeal(Long id) {
        Deal deal = dealMapper.selectById(id);
        if (deal == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "好价不存在");
        }
        return deal;
    }

    private AdminDealRow requireRow(Long id) {
        AdminDealRow row = dealMapper.selectAdminById(id);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "好价不存在");
        }
        return row;
    }

    private static int requireVersion(Integer version) {
        if (version == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "版本号不能为空");
        }
        return version;
    }

    private static Long numericId(String keyword) {
        if (keyword == null || keyword.length() > 18 || !keyword.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.valueOf(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private AdminDealResponse toResponse(AdminDealRow row) {
        boolean demo = DealSource.DEMO.equals(row.getSourceType()) || DealSource.isDemoProductCode(row.getProductCode());
        return new AdminDealResponse(
                row.getId(),
                row.getProductId(),
                row.getProductCode(),
                row.getProductStatus(),
                row.getProductDeleted() != null && row.getProductDeleted() == 1,
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
                row.getPurchaseUrl(),
                row.getPromotionUrl(),
                Boolean.TRUE.equals(row.getFeatured()),
                DbTimes.toOffset(row.getStartsAt()),
                DbTimes.toOffset(row.getExpiresAt()),
                DbTimes.toOffset(row.getVerifiedAt()),
                row.getStatus(),
                DbTimes.toOffset(row.getPublishedAt()),
                DbTimes.toOffset(row.getCreatedAt()),
                DbTimes.toOffset(row.getUpdatedAt()),
                row.getRejectReason(),
                row.getOfflineReason(),
                row.getSubmittedBy(),
                row.getSubmitterName(),
                row.getReviewedBy(),
                row.getReviewerName(),
                DbTimes.toOffset(row.getReviewedAt()),
                row.getVersion() == null ? 0 : row.getVersion(),
                demo);
    }

    private DealReviewResponse toReview(DealReviewRow row) {
        return new DealReviewResponse(
                row.getId(),
                row.getDealId(),
                row.getAction(),
                row.getReviewerId(),
                row.getReviewerName(),
                row.getReason(),
                DbTimes.toOffset(row.getCreatedAt()));
    }
}
