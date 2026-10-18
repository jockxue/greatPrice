package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.admin.AdminDealQuery;
import com.greatprice.dto.admin.AdminDealResponse;
import com.greatprice.service.DealAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/reviews")
public class ReviewAdminController {

    private final DealAdminService dealAdminService;

    public ReviewAdminController(DealAdminService dealAdminService) {
        this.dealAdminService = dealAdminService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminDealResponse>> pending(@Valid AdminDealQuery query) {
        return ApiResponse.ok(dealAdminService.pending(query));
    }
}
