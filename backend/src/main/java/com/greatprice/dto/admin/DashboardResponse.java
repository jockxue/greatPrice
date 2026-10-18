package com.greatprice.dto.admin;

import java.util.List;

public record DashboardResponse(
        long productCount,
        long publishedCount,
        long pendingCount,
        long offlineCount,
        long expiredCount,
        List<AdminDealResponse> recentDeals) {
}
