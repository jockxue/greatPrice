package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.common.web.DbTimes;
import com.greatprice.dto.admin.DashboardResponse;
import com.greatprice.entity.Deal;
import com.greatprice.entity.Product;
import com.greatprice.mapper.DealMapper;
import com.greatprice.mapper.ProductMapper;
import com.greatprice.security.AdminActors;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final ProductMapper productMapper;
    private final DealMapper dealMapper;
    private final DealAdminService dealAdminService;
    private final DealExpiryService dealExpiryService;
    private final AdminActors adminActors;

    public DashboardService(ProductMapper productMapper, DealMapper dealMapper,
                            DealAdminService dealAdminService, DealExpiryService dealExpiryService,
                            AdminActors adminActors) {
        this.productMapper = productMapper;
        this.dealMapper = dealMapper;
        this.dealAdminService = dealAdminService;
        this.dealExpiryService = dealExpiryService;
        this.adminActors = adminActors;
    }

    public DashboardResponse load() {
        adminActors.current();
        LocalDateTime now = DbTimes.now();
        long productCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery().eq(Product::getDeleted, 0));
        long pendingCount = dealMapper.selectCount(Wrappers.<Deal>lambdaQuery().eq(Deal::getStatus, Deal.STATUS_PENDING_REVIEW));
        long offlineCount = dealMapper.selectCount(Wrappers.<Deal>lambdaQuery().eq(Deal::getStatus, Deal.STATUS_OFFLINE));
        return new DashboardResponse(
                productCount,
                dealMapper.countPublishedActive(now),
                pendingCount,
                offlineCount,
                dealMapper.countExpired(now),
                dealAdminService.recent(),
                dealExpiryService.latest());
    }
}
