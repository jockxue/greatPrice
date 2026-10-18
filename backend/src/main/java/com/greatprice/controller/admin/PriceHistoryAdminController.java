package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.admin.PriceHistoryQuery;
import com.greatprice.dto.admin.PriceHistoryResponse;
import com.greatprice.dto.admin.PriceVerifyRequest;
import com.greatprice.service.PriceHistoryAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/price-history")
public class PriceHistoryAdminController {

    private final PriceHistoryAdminService priceHistoryAdminService;

    public PriceHistoryAdminController(PriceHistoryAdminService priceHistoryAdminService) {
        this.priceHistoryAdminService = priceHistoryAdminService;
    }

    @GetMapping
    public ApiResponse<PageResult<PriceHistoryResponse>> page(@Valid PriceHistoryQuery query) {
        return ApiResponse.ok(priceHistoryAdminService.page(query));
    }

    @PostMapping
    public ApiResponse<PriceHistoryResponse> verify(@Valid @RequestBody PriceVerifyRequest request) {
        return ApiResponse.ok(priceHistoryAdminService.verify(request));
    }
}
