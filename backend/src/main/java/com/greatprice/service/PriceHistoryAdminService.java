package com.greatprice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.api.PageResult;
import com.greatprice.common.exception.BizException;
import com.greatprice.common.web.DbTimes;
import com.greatprice.common.web.MoneyText;
import com.greatprice.dto.admin.PriceHistoryQuery;
import com.greatprice.dto.admin.PriceHistoryResponse;
import com.greatprice.dto.admin.PriceVerifyRequest;
import com.greatprice.entity.Deal;
import com.greatprice.entity.DealSource;
import com.greatprice.entity.PriceHistory;
import com.greatprice.entity.PriceHistoryRow;
import com.greatprice.entity.Product;
import com.greatprice.mapper.DealMapper;
import com.greatprice.mapper.PriceHistoryMapper;
import com.greatprice.mapper.ProductMapper;
import com.greatprice.security.AdminActors;
import com.greatprice.security.AdminUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PriceHistoryAdminService {

    private final PriceHistoryMapper priceHistoryMapper;
    private final ProductMapper productMapper;
    private final DealMapper dealMapper;
    private final AuditService auditService;
    private final AdminActors adminActors;

    public PriceHistoryAdminService(PriceHistoryMapper priceHistoryMapper, ProductMapper productMapper,
                                    DealMapper dealMapper, AuditService auditService, AdminActors adminActors) {
        this.priceHistoryMapper = priceHistoryMapper;
        this.productMapper = productMapper;
        this.dealMapper = dealMapper;
        this.auditService = auditService;
        this.adminActors = adminActors;
    }

    public PageResult<PriceHistoryResponse> page(PriceHistoryQuery query) {
        adminActors.current();
        if (query.productId() == null && query.dealId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请指定商品或好价");
        }
        IPage<PriceHistoryRow> page = priceHistoryMapper.selectAdminPage(
                Page.of(query.page(), query.size()), query.productId(), query.dealId());
        return PageResult.of(page, this::toResponse);
    }

    @Transactional
    public PriceHistoryResponse verify(PriceVerifyRequest request) {
        AdminUserDetails actor = adminActors.current();
        if (!DealSource.VERIFIABLE.contains(request.sourceType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "演示数据不能写入真实价格历史");
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null || product.getDeleted() == null || product.getDeleted() != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品不存在");
        }
        if (DealSource.isDemoProductCode(product.getProductCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "演示数据不能写入真实价格历史");
        }
        Deal deal = null;
        if (request.dealId() != null) {
            deal = dealMapper.selectById(request.dealId());
            if (deal == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "好价不存在");
            }
            if (!product.getId().equals(deal.getProductId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "好价与商品不匹配");
            }
            if (DealSource.DEMO.equals(deal.getSourceType())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "演示数据不能写入真实价格历史");
            }
        }
        BigDecimal price = MoneyText.parse(request.price(), "核验价格", true);
        LocalDateTime recordedAt = DbTimes.now();
        PriceHistory history = new PriceHistory();
        history.setProductId(product.getId());
        history.setDealId(deal == null ? null : deal.getId());
        history.setPrice(price);
        history.setCurrency(deal == null || deal.getCurrency() == null ? "CNY" : deal.getCurrency());
        history.setSourceType(request.sourceType());
        history.setSourceName(request.sourceName().strip());
        history.setVerificationStatus(PriceHistory.MANUAL_VERIFIED);
        history.setRecordedAt(recordedAt);
        history.setVerifiedBy(actor.getId());
        history.setNote(request.note() == null || request.note().isBlank() ? null : request.note().strip());
        priceHistoryMapper.insert(history);
        if (deal != null && dealMapper.markVerified(deal.getId(), recordedAt) != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "演示数据不能写入真实价格历史");
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.PRICE_VERIFY, "PRICE_HISTORY", history.getId(),
                "price=" + price.toPlainString());
        return toResponse(priceHistoryMapper.selectAdminById(history.getId()));
    }

    private PriceHistoryResponse toResponse(PriceHistoryRow row) {
        return new PriceHistoryResponse(
                row.getId(),
                row.getProductId(),
                row.getProductCode(),
                row.getProductTitle(),
                row.getDealId(),
                row.getPrice(),
                row.getCurrency(),
                row.getSourceType(),
                row.getSourceName(),
                row.getVerificationStatus(),
                DbTimes.toOffset(row.getRecordedAt()),
                row.getVerifiedBy(),
                row.getVerifierName(),
                row.getNote());
    }
}
