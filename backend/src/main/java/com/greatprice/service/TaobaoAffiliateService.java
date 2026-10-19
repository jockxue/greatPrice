package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.databind.JsonNode;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.api.PageResult;
import com.greatprice.common.exception.BizException;
import com.greatprice.common.web.DbTimes;
import com.greatprice.dto.admin.DealWriteRequest;
import com.greatprice.dto.admin.ProductWriteRequest;
import com.greatprice.dto.admin.TaobaoImportRequest;
import com.greatprice.dto.admin.TaobaoImportResponse;
import com.greatprice.dto.admin.TaobaoItemRequest;
import com.greatprice.dto.admin.TaobaoItemResponse;
import com.greatprice.dto.admin.TaobaoJobQuery;
import com.greatprice.dto.admin.TaobaoJobResponse;
import com.greatprice.dto.admin.TaobaoProbeResponse;
import com.greatprice.dto.admin.TaobaoPromotionRequest;
import com.greatprice.dto.admin.TaobaoPromotionResponse;
import com.greatprice.dto.admin.TaobaoSearchQuery;
import com.greatprice.dto.admin.TaobaoSearchResponse;
import com.greatprice.dto.admin.TaobaoStatusResponse;
import com.greatprice.dto.admin.TaobaoTestResponse;
import com.greatprice.entity.Category;
import com.greatprice.entity.Deal;
import com.greatprice.entity.DealSource;
import com.greatprice.entity.JobRun;
import com.greatprice.entity.Product;
import com.greatprice.entity.ProductExternalRef;
import com.greatprice.integration.taobao.TaobaoCallException;
import com.greatprice.integration.taobao.TaobaoErrors;
import com.greatprice.integration.taobao.TaobaoIds;
import com.greatprice.integration.taobao.TaobaoLinks;
import com.greatprice.integration.taobao.TaobaoMoney;
import com.greatprice.integration.taobao.TaobaoPayloads;
import com.greatprice.integration.taobao.TaobaoProperties;
import com.greatprice.integration.taobao.TaobaoTexts;
import com.greatprice.integration.taobao.TaobaoTopClient;
import com.greatprice.mapper.CategoryMapper;
import com.greatprice.mapper.DealMapper;
import com.greatprice.mapper.JobRunMapper;
import com.greatprice.mapper.ProductExternalRefMapper;
import com.greatprice.mapper.ProductMapper;
import com.greatprice.security.AdminActors;
import com.greatprice.security.AdminUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaobaoAffiliateService {

    static final String JOB_SEARCH = "TAOBAO_SEARCH";
    static final String JOB_DETAIL = "TAOBAO_ITEM_INFO";
    static final String JOB_PROMOTION = "TAOBAO_PROMOTION";
    static final String JOB_IMPORT = "TAOBAO_IMPORT";
    static final String JOB_REFRESH = "TAOBAO_REFRESH";
    static final String JOB_TEST = "TAOBAO_API_TEST";

    /** 官方参数 platform：1 为 PC，2 为无线。导购场景使用无线。 */
    private static final String WIRELESS = "2";
    private static final String SEP = "\u001f";
    private static final Set<String> SORTS = Set.of(
            "total_sales_des", "total_sales_asc",
            "tk_rate_des", "tk_rate_asc",
            "tk_total_sales_des", "tk_total_sales_asc",
            "tk_total_commi_des", "tk_total_commi_asc",
            "price_des", "price_asc",
            "match_des", "match_asc");
    private static final Set<String> JOBS = Set.of(JOB_SEARCH, JOB_DETAIL, JOB_PROMOTION, JOB_IMPORT, JOB_REFRESH, JOB_TEST);
    private static final Set<String> EDITABLE = Set.of(Deal.STATUS_DRAFT, Deal.STATUS_PENDING_REVIEW);
    private static final Logger log = LoggerFactory.getLogger(TaobaoAffiliateService.class);

    private final TaobaoProperties properties;
    private final TaobaoTopClient client;
    private final ProductAdminService productAdminService;
    private final DealAdminService dealAdminService;
    private final ProductExternalRefMapper externalRefMapper;
    private final ProductMapper productMapper;
    private final DealMapper dealMapper;
    private final CategoryMapper categoryMapper;
    private final JobRunMapper jobRunMapper;
    private final AuditService auditService;
    private final AdminActors adminActors;
    private final TransactionTemplate transactions;

    public TaobaoAffiliateService(TaobaoProperties properties, TaobaoTopClient client,
                                  ProductAdminService productAdminService, DealAdminService dealAdminService,
                                  ProductExternalRefMapper externalRefMapper, ProductMapper productMapper,
                                  DealMapper dealMapper, CategoryMapper categoryMapper, JobRunMapper jobRunMapper,
                                  AuditService auditService, AdminActors adminActors,
                                  PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.client = client;
        this.productAdminService = productAdminService;
        this.dealAdminService = dealAdminService;
        this.externalRefMapper = externalRefMapper;
        this.productMapper = productMapper;
        this.dealMapper = dealMapper;
        this.categoryMapper = categoryMapper;
        this.jobRunMapper = jobRunMapper;
        this.auditService = auditService;
        this.adminActors = adminActors;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public TaobaoStatusResponse status() {
        adminActors.current();
        JobRun latest = latestJob(JOB_TEST);
        return new TaobaoStatusResponse(
                properties.isEnabled(),
                properties.isManualSyncEnabled(),
                TaobaoProperties.present(properties.getAppKey()),
                properties.maskedAppKey(),
                TaobaoProperties.present(properties.getAppSecret()),
                TaobaoProperties.positiveNumber(properties.getSiteId()),
                TaobaoProperties.positiveNumber(properties.getAdzoneId()),
                TaobaoProperties.present(properties.getSession()),
                properties.gatewayHost(),
                properties.searchReady(),
                properties.detailReady(),
                properties.promotionReady(),
                latest == null ? null : latest.getStatus(),
                latest == null ? null : latest.getMessage(),
                latest == null ? null : latest.getErrorCode(),
                latest == null ? null : DbTimes.toOffset(latest.getFinishedAt()));
    }

    public TaobaoTestResponse test() {
        adminActors.current();
        requireManualSwitch();
        LocalDateTime started = DbTimes.now();
        TaobaoProbeResponse search = probeSearch();
        String itemId = null;
        if ("SUCCESS".equals(search.state()) && search.message() != null && search.message().startsWith("item=")) {
            itemId = search.message().substring("item=".length());
        }
        TaobaoProbeResponse detail = probeDetail(itemId, search);
        TaobaoProbeResponse promotion = probePromotion(itemId, search);
        boolean failed = "FAILED".equals(search.state()) || "FAILED".equals(detail.state()) || "FAILED".equals(promotion.state());
        boolean retryable = search.retryable() || detail.retryable() || promotion.retryable();
        String errorCode = firstCode(search, detail, promotion);
        record(JOB_TEST, started, !failed, 1, failed ? 0 : 1, failed ? 1 : 0, errorCode, retryable && failed, "TEST",
                "search=" + search.state() + ";detail=" + detail.state() + ";promotion=" + promotion.state());
        return new TaobaoTestResponse(displayProbe(search), detail, promotion);
    }

    public TaobaoSearchResponse search(TaobaoSearchQuery query) {
        adminActors.current();
        requireSearch();
        LocalDateTime started = DbTimes.now();
        String payload = payload(JOB_SEARCH, query.keyword(), String.valueOf(query.page()), String.valueOf(query.size()),
                empty(query.sort()), empty(query.startPrice()), empty(query.endPrice()),
                query.hasCoupon() == null ? "" : query.hasCoupon().toString());
        try {
            TaobaoSearchResponse response = searchResponse(fetchSearch(query));
            record(JOB_SEARCH, started, true, 1, 1, 0, null, false, null, "keyword=" + query.keyword());
            return response;
        } catch (RuntimeException ex) {
            recordFailure(JOB_SEARCH, started, payload, ex);
            throw ex;
        }
    }

    public TaobaoSearchResponse detail(String itemId) {
        adminActors.current();
        requireDetail();
        String id = TaobaoIds.requireItemId(itemId);
        LocalDateTime started = DbTimes.now();
        String payload = payload(JOB_DETAIL, id);
        try {
            TaobaoPayloads.Detail loaded = fetchDetail(id);
            if (loaded.item() == null) {
                throw new TaobaoCallException(ErrorCode.NOT_FOUND, "淘宝联盟未返回该商品", null, false);
            }
            record(JOB_DETAIL, started, true, 1, 1, 0, null, false, null, "itemId=" + id);
            return new TaobaoSearchResponse(TaobaoPayloads.DETAIL_METHOD, loaded.requestId(), 1, List.of(toItem(loaded)));
        } catch (RuntimeException ex) {
            recordFailure(JOB_DETAIL, started, payload, ex);
            throw ex;
        }
    }

    public TaobaoPromotionResponse promotion(TaobaoPromotionRequest request) {
        adminActors.current();
        requirePromotion();
        String itemId = TaobaoIds.requireItemId(request.itemId());
        LocalDateTime started = DbTimes.now();
        String payload = payload(JOB_PROMOTION, itemId, request.dealId() == null ? "" : request.dealId().toString());
        try {
            TaobaoPayloads.Promotion loaded = fetchPromotion(itemId);
            ChosenLink link = chooseLink(loaded);
            boolean persisted = persistLink(itemId, request.dealId(), link.url());
            record(JOB_PROMOTION, started, true, 1, 1, 0, null, false, null, "itemId=" + itemId);
            String note = persisted
                    ? TaobaoLinks.MINI_PROGRAM_NOTE
                    : "推广链接未写入站内好价。" + TaobaoLinks.MINI_PROGRAM_NOTE;
            return new TaobaoPromotionResponse(
                    TaobaoPayloads.PROMOTION_METHOD,
                    loaded.requestId(),
                    itemId,
                    link.url(),
                    link.field(),
                    loaded.couponInfo(),
                    loaded.couponStart(),
                    loaded.couponEnd(),
                    persisted,
                    request.dealId(),
                    false,
                    note);
        } catch (RuntimeException ex) {
            recordFailure(JOB_PROMOTION, started, payload, ex);
            throw ex;
        }
    }

    public TaobaoImportResponse importItem(TaobaoImportRequest request) {
        adminActors.current();
        requireDetail();
        String itemId = TaobaoIds.requireItemId(request.itemId());
        LocalDateTime started = DbTimes.now();
        String payload = payload(JOB_IMPORT, itemId, request.categoryId().toString());
        try {
            TaobaoImportResponse response = save(itemId, fetchDetail(itemId), request.categoryId(), false);
            record(JOB_IMPORT, started, true, 1, 1, 0, null, false, null, "itemId=" + itemId);
            return response;
        } catch (RuntimeException ex) {
            recordFailure(JOB_IMPORT, started, payload, ex);
            throw ex;
        }
    }

    public TaobaoImportResponse refresh(TaobaoItemRequest request) {
        adminActors.current();
        requireDetail();
        String itemId = TaobaoIds.requireItemId(request.itemId());
        LocalDateTime started = DbTimes.now();
        String payload = payload(JOB_REFRESH, itemId);
        try {
            TaobaoPayloads.Detail loaded = fetchDetail(itemId);
            if (findRef(itemId) == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "该淘宝商品尚未导入");
            }
            TaobaoImportResponse response = save(itemId, loaded, null, true);
            record(JOB_REFRESH, started, true, 1, 1, 0, null, false, null, "itemId=" + itemId);
            return response;
        } catch (RuntimeException ex) {
            recordFailure(JOB_REFRESH, started, payload, ex);
            throw ex;
        }
    }

    public PageResult<TaobaoJobResponse> jobs(TaobaoJobQuery query) {
        adminActors.current();
        if (query.status() != null && !JobRun.SUCCESS.equals(query.status()) && !JobRun.FAILED.equals(query.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务状态不合法");
        }
        if (query.jobName() != null && !JOBS.contains(query.jobName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务类型不合法");
        }
        var page = jobRunMapper.selectPage(Page.of(query.page(), query.size()), Wrappers.<JobRun>lambdaQuery()
                .in(JobRun::getJobName, JOBS)
                .eq(query.status() != null, JobRun::getStatus, query.status())
                .eq(query.jobName() != null, JobRun::getJobName, query.jobName())
                .orderByDesc(JobRun::getId));
        return PageResult.of(page, this::toJob);
    }

    public TaobaoJobResponse retry(Long id) {
        adminActors.current();
        JobRun job = jobRunMapper.selectById(id);
        if (job == null || job.getJobName() == null || !JOBS.contains(job.getJobName())) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (!Integer.valueOf(1).equals(job.getRetryable()) || !JobRun.FAILED.equals(job.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该失败不能重试");
        }
        dispatch(job.getRetryPayload());
        JobRun latest = latestJob(job.getJobName());
        return latest == null ? toJob(job) : toJob(latest);
    }

    private void dispatch(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split(SEP, -1);
        if (parts.length == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该失败不能重试");
        }
        switch (parts[0]) {
            case JOB_SEARCH -> search(new TaobaoSearchQuery(parts[1], integer(parts, 2), integer(parts, 3),
                    emptyToNull(parts, 5), emptyToNull(parts, 6), emptyToNull(parts, 4), boolArg(parts, 7)));
            case JOB_DETAIL -> detail(parts[1]);
            case JOB_PROMOTION -> promotion(new TaobaoPromotionRequest(parts[1], longArg(parts, 2)));
            case JOB_IMPORT -> importItem(new TaobaoImportRequest(parts[1], longArg(parts, 2)));
            case JOB_REFRESH -> refresh(new TaobaoItemRequest(parts[1]));
            case JOB_TEST -> test();
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "该失败不能重试");
        }
    }

    private TaobaoPayloads.SearchPage fetchSearch(TaobaoSearchQuery query) {
        if (query.keyword().chars().anyMatch(ch -> ch < 0x20 || ch == 0x7F)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "关键词包含无法提交的字符");
        }
        if (query.sort() != null && !SORTS.contains(query.sort())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "排序方式不在淘宝接口允许的范围内");
        }
        String start = priceParam(query.startPrice(), "最低价");
        String end = priceParam(query.endPrice(), "最高价");
        if (start != null && end != null && new BigDecimal(start).compareTo(new BigDecimal(end)) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "最低价不能高于最高价");
        }
        Map<String, String> business = new LinkedHashMap<>();
        business.put("q", query.keyword());
        business.put("page_no", query.page().toString());
        business.put("page_size", query.size().toString());
        business.put("adzone_id", properties.getAdzoneId().strip());
        business.put("platform", WIRELESS);
        business.put("start_price", start);
        business.put("end_price", end);
        business.put("sort", query.sort());
        if (query.hasCoupon() != null) {
            business.put("has_coupon", query.hasCoupon().toString());
        }
        JsonNode body = client.call(TaobaoPayloads.SEARCH_METHOD, business, false);
        TaobaoPayloads.SearchPage page = TaobaoPayloads.search(body);
        throwIfFailed(page.failure());
        return page;
    }

    private TaobaoPayloads.Detail fetchDetail(String itemId) {
        Map<String, String> business = new LinkedHashMap<>();
        business.put("num_iids", itemId);
        business.put("platform", WIRELESS);
        JsonNode body = client.call(TaobaoPayloads.DETAIL_METHOD, business, false);
        TaobaoPayloads.Detail detail = TaobaoPayloads.detail(body, itemId);
        throwIfFailed(detail.failure());
        return detail;
    }

    private TaobaoPayloads.Promotion fetchPromotion(String itemId) {
        Map<String, String> business = new LinkedHashMap<>();
        business.put("item_id", itemId);
        business.put("adzone_id", properties.getAdzoneId().strip());
        business.put("site_id", properties.getSiteId().strip());
        business.put("platform", WIRELESS);
        JsonNode body = client.call(TaobaoPayloads.PROMOTION_METHOD, business, true);
        TaobaoPayloads.Promotion promotion = TaobaoPayloads.promotion(body);
        throwIfFailed(promotion.failure());
        return promotion;
    }

    private TaobaoSearchResponse searchResponse(TaobaoPayloads.SearchPage page) {
        List<TaobaoItemResponse> items = new ArrayList<>();
        for (TaobaoPayloads.SearchHit hit : page.hits()) {
            items.add(toItem(hit, null));
        }
        return new TaobaoSearchResponse(TaobaoPayloads.SEARCH_METHOD, page.requestId(), page.total(), items);
    }

    private TaobaoItemResponse toItem(TaobaoPayloads.Detail detail) {
        TaobaoItemResponse base = toItem(detail.item(), detail);
        return base;
    }

    private TaobaoItemResponse toItem(TaobaoPayloads.SearchHit hit, TaobaoPayloads.Detail detail) {
        TaobaoMoney.Parsed reserve = TaobaoMoney.parse(hit.reservePrice());
        TaobaoMoney.Parsed finalPrice = TaobaoMoney.parse(hit.finalPrice());
        TaobaoMoney.Parsed sale = TaobaoMoney.parse(hit.salePrice());
        ProductExternalRef ref = hit.itemId() == null ? null : findRef(hit.itemId());
        Deal deal = ref == null || hit.itemId() == null ? null : findDeal(ref.getProductId(), TaobaoIds.offerKey(hit.itemId()));
        return new TaobaoItemResponse(
                TaobaoIds.PLATFORM,
                detail == null ? TaobaoPayloads.SEARCH_METHOD : TaobaoPayloads.DETAIL_METHOD,
                hit.itemId(),
                hit.title(),
                TaobaoLinks.image(hit.imageUrl()),
                hit.shopTitle(),
                reserve.value(),
                reserve.status(),
                finalPrice.value(),
                finalPrice.status(),
                sale.value(),
                sale.status(),
                hit.couponId(),
                hit.couponInfo(),
                hit.couponStart(),
                hit.couponEnd(),
                TaobaoLinks.purchase(hit.itemUrl()),
                TaobaoLinks.purchase(hit.clickUrl()),
                false,
                hit.categoryName(),
                suggestCategory(hit.categoryName()),
                ref == null ? null : ref.getProductId(),
                deal == null ? null : deal.getId(),
                deal == null ? null : deal.getStatus(),
                detail == null ? null : detail.volume(),
                detail == null ? null : detail.userType(),
                detail == null ? null : detail.catName(),
                detail == null ? null : detail.catLeafName(),
                detail == null ? null : detail.playInfo(),
                detail == null ? null : detail.presaleText(),
                detail == null ? null : detail.kuadian());
    }

    private TaobaoImportResponse save(String requestedItemId, TaobaoPayloads.Detail detail, Long categoryId, boolean refresh) {
        if (detail.item() == null) {
            throw new TaobaoCallException(ErrorCode.NOT_FOUND, "淘宝联盟未返回该商品", null, false);
        }
        String itemId = requestedItemId;
        ProductLink product = refresh ? existingProduct(itemId) : upsertProduct(detail, categoryId, itemId);
        guardDemo(product.productId());
        if (refresh) {
            refreshProductText(product.productId(), itemId, detail);
        }
        DealLink deal = upsertDeal(product.productId(), itemId, detail, refresh);
        String message = dealMessage(product, deal);
        return new TaobaoImportResponse(
                product.created() ? "CREATED" : "ALREADY_EXISTS",
                product.productId(),
                product.productCode(),
                deal.outcome(),
                deal.dealId(),
                deal.status(),
                message);
    }

    private ProductLink existingProduct(String itemId) {
        ProductExternalRef ref = findRef(itemId);
        if (ref == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "该淘宝商品尚未导入");
        }
        Product product = productMapper.selectById(ref.getProductId());
        if (product == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "站内商品不存在");
        }
        return new ProductLink(product.getId(), product.getProductCode(), false);
    }

    private ProductLink upsertProduct(TaobaoPayloads.Detail detail, Long categoryId, String itemId) {
        try {
            return transactions.execute(status -> {
                ProductExternalRef existing = findRef(itemId);
                if (existing != null) {
                    Product product = productMapper.selectById(existing.getProductId());
                    return new ProductLink(existing.getProductId(), product == null ? null : product.getProductCode(), false);
                }
                var created = productAdminService.create(new ProductWriteRequest(
                        TaobaoIds.productCode(itemId),
                        requireTitle(detail.item().title()),
                        null,
                        categoryId,
                        TaobaoLinks.image(detail.item().imageUrl()),
                        null,
                        null));
                ProductExternalRef ref = new ProductExternalRef();
                ref.setProductId(created.id());
                ref.setPlatform(TaobaoIds.PLATFORM);
                ref.setExternalItemId(itemId);
                externalRefMapper.insert(ref);
                return new ProductLink(created.id(), created.productCode(), true);
            });
        } catch (BizException ex) {
            if (!"商品编码已存在".equals(ex.getMessage())) {
                throw ex;
            }
            ProductExternalRef ref = findRef(itemId);
            if (ref == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "商品编码已被站内占用，未关联该淘宝商品");
            }
            Product product = productMapper.selectById(ref.getProductId());
            return new ProductLink(ref.getProductId(), product == null ? null : product.getProductCode(), false);
        } catch (DataIntegrityViolationException ex) {
            ProductExternalRef ref = findRef(itemId);
            if (ref == null) {
                throw new BizException(ErrorCode.CONFLICT, "商品正在导入，请稍后重试");
            }
            Product product = productMapper.selectById(ref.getProductId());
            return new ProductLink(ref.getProductId(), product == null ? null : product.getProductCode(), false);
        }
    }

    private void refreshProductText(long productId, String itemId, TaobaoPayloads.Detail detail) {
        String title = requireTitle(detail.item().title());
        String image = TaobaoLinks.image(detail.item().imageUrl());
        var update = Wrappers.<Product>lambdaUpdate()
                .eq(Product::getId, productId)
                .eq(Product::getDeleted, 0)
                .set(Product::getTitle, title);
        if (image != null) {
            update.set(Product::getImageUrl, image);
        }
        productMapper.update(null, update);
        AdminUserDetails actor = adminActors.current();
        auditService.record(actor.getId(), actor.getUsername(), AuditService.TAOBAO_REFRESH, "PRODUCT", productId, itemId);
    }

    private DealLink upsertDeal(long productId, String itemId, TaobaoPayloads.Detail detail, boolean refresh) {
        String offerKey = TaobaoIds.offerKey(itemId);
        Deal existing = findDeal(productId, offerKey);
        PriceChoice price = prices(detail);
        if (existing != null && (!refresh || !EDITABLE.contains(existing.getStatus()))) {
            return new DealLink("ALREADY_EXISTS", existing.getId(), existing.getStatus());
        }
        if (price == null) {
            return new DealLink(existing == null ? "SKIPPED_NO_PRICE" : "SKIPPED_NO_PRICE",
                    existing == null ? null : existing.getId(),
                    existing == null ? null : existing.getStatus());
        }
        DealWriteRequest request = dealRequest(productId, detail, price, existing);
        if (existing == null) {
            try {
                var created = dealAdminService.createAffiliateDraft(request, offerKey, null, null);
                return new DealLink("CREATED", created.id(), created.status());
            } catch (BizException ex) {
                if (!ErrorCode.CONFLICT.equals(ex.getErrorCode())) {
                    throw ex;
                }
                Deal winner = findDeal(productId, offerKey);
                if (winner == null) {
                    throw ex;
                }
                return new DealLink("ALREADY_EXISTS", winner.getId(), winner.getStatus());
            }
        }
        var updated = dealAdminService.update(existing.getId(), request);
        return new DealLink("UPDATED", updated.id(), updated.status());
    }

    private DealWriteRequest dealRequest(long productId, TaobaoPayloads.Detail detail, PriceChoice price, Deal existing) {
        OffsetDateTime starts = millis(detail.presaleStart());
        OffsetDateTime ends = millis(detail.presaleEnd());
        if (starts == null || ends == null || !starts.isBefore(ends)) {
            starts = existing == null ? null : DbTimes.toOffset(existing.getStartsAt());
            ends = existing == null ? null : DbTimes.toOffset(existing.getExpiresAt());
        }
        String purchase = TaobaoLinks.purchase(detail.item().itemUrl());
        if (purchase == null && existing != null) {
            purchase = existing.getPurchaseUrl();
        }
        return new DealWriteRequest(
                productId,
                price.current(),
                price.original(),
                "CNY",
                description(detail, price.note()),
                DealSource.AFFILIATE,
                shopName(detail.nick()),
                purchase,
                existing == null ? null : existing.getPromotionUrl(),
                starts,
                ends,
                existing == null ? null : existing.getVersion());
    }

    private boolean persistLink(String itemId, Long dealId, String url) {
        if (dealId == null) {
            return false;
        }
        ProductExternalRef ref = findRef(itemId);
        if (ref == null) {
            return false;
        }
        Deal deal = dealMapper.selectById(dealId);
        if (deal == null || !ref.getProductId().equals(deal.getProductId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "好价与该淘宝商品不匹配");
        }
        if (!EDITABLE.contains(deal.getStatus())) {
            return false;
        }
        int rows = dealMapper.update(null, Wrappers.<Deal>lambdaUpdate()
                .eq(Deal::getId, dealId)
                .eq(Deal::getProductId, ref.getProductId())
                .in(Deal::getStatus, EDITABLE)
                .set(Deal::getPromotionUrl, url)
                .set(Deal::getPromotionSource, TaobaoPayloads.PROMOTION_METHOD)
                .set(Deal::getPromotionGeneratedAt, DbTimes.now())
                .setSql("version = version + 1"));
        if (rows == 1) {
            AdminUserDetails actor = adminActors.current();
            auditService.record(actor.getId(), actor.getUsername(), AuditService.TAOBAO_PROMOTION, "DEAL", dealId, itemId);
        }
        return rows == 1;
    }

    private ChosenLink chooseLink(TaobaoPayloads.Promotion promotion) {
        if (promotion.couponClickUrl() != null) {
            return new ChosenLink(TaobaoLinks.promotion(promotion.couponClickUrl()), "coupon_click_url");
        }
        if (promotion.itemUrl() != null) {
            return new ChosenLink(TaobaoLinks.promotion(promotion.itemUrl()), "item_url");
        }
        if (promotion.ysylClickUrl() != null) {
            return new ChosenLink(TaobaoLinks.promotion(promotion.ysylClickUrl()), "ysyl_click_url");
        }
        throw new TaobaoCallException(ErrorCode.UPSTREAM_ERROR, "淘宝联盟未返回推广链接", null, false);
    }

    private void guardDemo(long productId) {
        Product product = productMapper.selectById(productId);
        if (product != null && DealSource.isDemoProductCode(product.getProductCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "演示商品不能被淘宝同步覆盖");
        }
    }

    private Long suggestCategory(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<Category> found = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, name.strip())
                .eq(Category::getStatus, Category.STATUS_ENABLED));
        return found.size() == 1 ? found.get(0).getId() : null;
    }

    private ProductExternalRef findRef(String itemId) {
        return externalRefMapper.selectOne(Wrappers.<ProductExternalRef>lambdaQuery()
                .eq(ProductExternalRef::getPlatform, TaobaoIds.PLATFORM)
                .eq(ProductExternalRef::getExternalItemId, itemId));
    }

    private Deal findDeal(long productId, String offerKey) {
        return dealMapper.selectOne(Wrappers.<Deal>lambdaQuery()
                .eq(Deal::getProductId, productId)
                .eq(Deal::getSourceType, DealSource.AFFILIATE)
                .eq(Deal::getExternalOfferKey, offerKey));
    }

    private PriceChoice prices(TaobaoPayloads.Detail detail) {
        TaobaoMoney.Parsed zk = TaobaoMoney.parse(detail.item().finalPrice());
        TaobaoMoney.Parsed sale = TaobaoMoney.parse(detail.item().salePrice());
        TaobaoMoney.Parsed reserve = TaobaoMoney.parse(detail.item().reservePrice());
        String current;
        String note;
        if (TaobaoMoney.OK.equals(zk.status())) {
            current = zk.value();
            note = "当前价取自接口字段 zk_final_price";
        } else if (TaobaoMoney.OK.equals(sale.status())) {
            current = sale.value();
            note = "当前价取自接口字段 sale_price";
        } else {
            return null;
        }
        return new PriceChoice(current, TaobaoMoney.OK.equals(reserve.status()) ? reserve.value() : null, note);
    }

    private String description(TaobaoPayloads.Detail detail, String priceNote) {
        List<String> parts = new ArrayList<>();
        addText(parts, detail.playInfo());
        addText(parts, detail.presaleText());
        addText(parts, detail.kuadian());
        if (parts.isEmpty()) {
            parts.add("淘宝联盟商品详情未返回优惠券或活动说明");
        }
        parts.add(priceNote);
        String text = String.join("；", parts);
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    private static void addText(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.strip());
        }
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "接口未返回商品标题，未导入");
        }
        String value = title.strip();
        if (value.length() > 200) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "商品标题超过站内长度，未导入");
        }
        return value;
    }

    private static String shopName(String nick) {
        if (nick == null || nick.isBlank()) {
            return "淘宝";
        }
        String value = nick.strip();
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private static OffsetDateTime millis(String raw) {
        if (raw == null || !raw.matches("\\d{13}")) {
            return null;
        }
        return Instant.ofEpochMilli(Long.parseLong(raw)).atZone(DbTimes.ZONE).toOffsetDateTime();
    }

    private static String priceParam(String raw, String label) {
        if (raw == null) {
            return null;
        }
        TaobaoMoney.Parsed parsed = TaobaoMoney.parse(raw);
        if (!TaobaoMoney.OK.equals(parsed.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "不是合法金额");
        }
        return parsed.value();
    }

    private void throwIfFailed(TaobaoPayloads.Failure failure) {
        if (failure == null) {
            return;
        }
        String detail = failure.subMsg() == null ? "淘宝联盟返回失败" : failure.subMsg();
        StringBuilder suffix = new StringBuilder();
        if (failure.code() != null) {
            suffix.append("（").append(failure.code());
            if (failure.subCode() != null) {
                suffix.append("/").append(failure.subCode());
            }
            suffix.append("）");
        }
        if (failure.requestId() != null) {
            suffix.append(" requestId=").append(failure.requestId());
        }
        throw new TaobaoCallException(
                TaobaoErrors.toErrorCode(false, failure.code(), failure.subCode()),
                TaobaoTexts.redact(detail + suffix, properties.getAppSecret(), properties.getSession()),
                TaobaoTexts.limit(failure.code(), 64),
                failure.retryable());
    }

    private void requireSearch() {
        requireManualSwitch();
        if (!properties.searchReady()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "淘宝联盟配置不完整");
        }
    }

    private void requireDetail() {
        requireManualSwitch();
        if (!properties.detailReady()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "淘宝联盟配置不完整");
        }
    }

    private void requirePromotion() {
        requireManualSwitch();
        if (!properties.promotionReady()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "淘宝联盟配置不完整");
        }
    }

    private void requireManualSwitch() {
        if (!properties.isEnabled()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "淘宝联盟未启用");
        }
        if (!properties.isManualSyncEnabled()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "淘宝联盟手动同步未开启");
        }
    }

    private TaobaoProbeResponse probeSearch() {
        if (!properties.searchReady()) {
            return new TaobaoProbeResponse("NOT_CONFIGURED", TaobaoPayloads.SEARCH_METHOD, null, "商品搜索配置不完整", null, false);
        }
        try {
            TaobaoPayloads.SearchPage page = fetchSearch(new TaobaoSearchQuery("纸巾", 1, 1, null, null, null, null));
            String itemId = page.hits().isEmpty() ? null : page.hits().get(0).itemId();
            String message = itemId == null ? "搜索成功，没有返回商品" : "item=" + itemId;
            return new TaobaoProbeResponse("SUCCESS", TaobaoPayloads.SEARCH_METHOD, null, message, page.requestId(), false);
        } catch (TaobaoCallException ex) {
            return new TaobaoProbeResponse("FAILED", TaobaoPayloads.SEARCH_METHOD, ex.platformCode(), ex.getMessage(), null, ex.retryable());
        }
    }

    private TaobaoProbeResponse probeDetail(String itemId, TaobaoProbeResponse search) {
        if (!properties.detailReady()) {
            return new TaobaoProbeResponse("NOT_CONFIGURED", TaobaoPayloads.DETAIL_METHOD, null, "商品详情配置不完整", null, false);
        }
        if (!"SUCCESS".equals(search.state()) || itemId == null) {
            return new TaobaoProbeResponse("SKIPPED", TaobaoPayloads.DETAIL_METHOD, null, "没有可用于探测的商品", null, false);
        }
        try {
            TaobaoPayloads.Detail detail = fetchDetail(itemId);
            if (detail.item() == null) {
                return new TaobaoProbeResponse("FAILED", TaobaoPayloads.DETAIL_METHOD, null, "淘宝联盟未返回该商品", detail.requestId(), false);
            }
            return new TaobaoProbeResponse("SUCCESS", TaobaoPayloads.DETAIL_METHOD, null, "已返回商品详情", detail.requestId(), false);
        } catch (TaobaoCallException ex) {
            return new TaobaoProbeResponse("FAILED", TaobaoPayloads.DETAIL_METHOD, ex.platformCode(), ex.getMessage(), null, ex.retryable());
        }
    }

    private TaobaoProbeResponse probePromotion(String itemId, TaobaoProbeResponse search) {
        if (!properties.promotionReady()) {
            return new TaobaoProbeResponse("NOT_CONFIGURED", TaobaoPayloads.PROMOTION_METHOD, null, "推广链接配置不完整", null, false);
        }
        if (!"SUCCESS".equals(search.state()) || itemId == null) {
            return new TaobaoProbeResponse("SKIPPED", TaobaoPayloads.PROMOTION_METHOD, null, "没有可用于转链的商品", null, false);
        }
        try {
            TaobaoPayloads.Promotion promotion = fetchPromotion(itemId);
            chooseLink(promotion);
            return new TaobaoProbeResponse("SUCCESS", TaobaoPayloads.PROMOTION_METHOD, null, "已生成推广链接", promotion.requestId(), false);
        } catch (TaobaoCallException ex) {
            return new TaobaoProbeResponse("FAILED", TaobaoPayloads.PROMOTION_METHOD, ex.platformCode(), ex.getMessage(), null, ex.retryable());
        } catch (RuntimeException ex) {
            return new TaobaoProbeResponse("FAILED", TaobaoPayloads.PROMOTION_METHOD, null, ex.getMessage(), null, false);
        }
    }

    private TaobaoProbeResponse displayProbe(TaobaoProbeResponse probe) {
        if ("SUCCESS".equals(probe.state()) && probe.message() != null && probe.message().startsWith("item=")) {
            return new TaobaoProbeResponse(probe.state(), probe.apiMethod(), probe.errorCode(), "搜索成功", probe.requestId(), false);
        }
        return probe;
    }

    private void recordFailure(String jobName, LocalDateTime started, String payload, RuntimeException ex) {
        boolean retryable = ex instanceof TaobaoCallException call && call.retryable();
        String code = ex instanceof TaobaoCallException call ? call.platformCode() : null;
        record(jobName, started, false, 1, 0, 1, code, retryable, retryable ? payload : null,
                TaobaoTexts.redact(ex.getMessage(), properties.getAppSecret(), properties.getSession()));
    }

    private void record(String jobName, LocalDateTime started, boolean success, int requested, int processed, int failed,
                        String errorCode, boolean retryable, String payload, String message) {
        JobRun run = new JobRun();
        run.setJobName(jobName);
        run.setStatus(success ? JobRun.SUCCESS : JobRun.FAILED);
        run.setRequestedCount(requested);
        run.setProcessedCount(processed);
        run.setFailedCount(failed);
        run.setErrorCode(TaobaoTexts.limit(errorCode, 64));
        run.setRetryable(retryable ? 1 : 0);
        run.setRetryPayload(retryable ? TaobaoTexts.limit(payload, 500) : null);
        run.setMessage(TaobaoTexts.limit(TaobaoTexts.redact(message, properties.getAppSecret(), properties.getSession()), 500));
        run.setStartedAt(started);
        run.setFinishedAt(DbTimes.now());
        try {
            jobRunMapper.insert(run);
        } catch (RuntimeException ex) {
            log.warn("淘宝任务记录写入失败 job={}", jobName);
        }
    }

    private JobRun latestJob(String jobName) {
        return jobRunMapper.selectOne(Wrappers.<JobRun>lambdaQuery()
                .eq(JobRun::getJobName, jobName)
                .orderByDesc(JobRun::getId)
                .last("LIMIT 1"));
    }

    private TaobaoJobResponse toJob(JobRun run) {
        return new TaobaoJobResponse(
                run.getId(),
                run.getJobName(),
                run.getStatus(),
                run.getRequestedCount() == null ? 0 : run.getRequestedCount(),
                run.getProcessedCount() == null ? 0 : run.getProcessedCount(),
                run.getFailedCount() == null ? 0 : run.getFailedCount(),
                run.getErrorCode(),
                Integer.valueOf(1).equals(run.getRetryable()),
                run.getMessage(),
                DbTimes.toOffset(run.getStartedAt()),
                DbTimes.toOffset(run.getFinishedAt()));
    }

    private static String firstCode(TaobaoProbeResponse... probes) {
        for (TaobaoProbeResponse probe : probes) {
            if (probe.errorCode() != null) {
                return probe.errorCode();
            }
        }
        return null;
    }

    private static String dealMessage(ProductLink product, DealLink deal) {
        String productText = product.created() ? "已新建站内商品" : "站内已有该淘宝商品，未新建";
        String dealText = switch (deal.outcome()) {
            case "CREATED" -> "好价已存为草稿，不会直接发布";
            case "UPDATED" -> "已更新草稿或待审核好价，未写入价格历史";
            case "SKIPPED_NO_PRICE" -> "接口没有返回可用价格，未写入好价";
            default -> "优惠已存在，未覆盖。状态为 " + deal.status();
        };
        return productText + "。" + dealText;
    }

    private static String payload(String... parts) {
        return String.join(SEP, parts);
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String[] parts, int index) {
        if (parts.length <= index || parts[index].isBlank()) {
            return null;
        }
        return parts[index];
    }

    private static Integer integer(String[] parts, int index) {
        String value = emptyToNull(parts, index);
        return value == null ? null : Integer.valueOf(value);
    }

    private static Long longArg(String[] parts, int index) {
        String value = emptyToNull(parts, index);
        return value == null ? null : Long.valueOf(value);
    }

    private static Boolean boolArg(String[] parts, int index) {
        String value = emptyToNull(parts, index);
        return value == null ? null : Boolean.valueOf(value);
    }

    private record ProductLink(long productId, String productCode, boolean created) {
    }

    private record DealLink(String outcome, Long dealId, String status) {
    }

    private record PriceChoice(String current, String original, String note) {
    }

    private record ChosenLink(String url, String field) {
    }
}
