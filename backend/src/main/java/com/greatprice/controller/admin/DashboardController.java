package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.dto.admin.DashboardResponse;
import com.greatprice.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> load() {
        return ApiResponse.ok(dashboardService.load());
    }
}
