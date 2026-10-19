package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.admin.TaobaoImportRequest;
import com.greatprice.dto.admin.TaobaoImportResponse;
import com.greatprice.dto.admin.TaobaoItemRequest;
import com.greatprice.dto.admin.TaobaoJobQuery;
import com.greatprice.dto.admin.TaobaoJobResponse;
import com.greatprice.dto.admin.TaobaoPromotionRequest;
import com.greatprice.dto.admin.TaobaoPromotionResponse;
import com.greatprice.dto.admin.TaobaoSearchQuery;
import com.greatprice.dto.admin.TaobaoSearchResponse;
import com.greatprice.dto.admin.TaobaoStatusResponse;
import com.greatprice.dto.admin.TaobaoTestResponse;
import com.greatprice.service.TaobaoAffiliateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/affiliate/taobao")
public class TaobaoAffiliateAdminController {

    private final TaobaoAffiliateService taobaoAffiliateService;

    public TaobaoAffiliateAdminController(TaobaoAffiliateService taobaoAffiliateService) {
        this.taobaoAffiliateService = taobaoAffiliateService;
    }

    @GetMapping("/status")
    public ApiResponse<TaobaoStatusResponse> status() {
        return ApiResponse.ok(taobaoAffiliateService.status());
    }

    @PostMapping("/tests")
    public ApiResponse<TaobaoTestResponse> test() {
        return ApiResponse.ok(taobaoAffiliateService.test());
    }

    @GetMapping("/items")
    public ApiResponse<TaobaoSearchResponse> items(@Valid TaobaoSearchQuery query) {
        return ApiResponse.ok(taobaoAffiliateService.search(query));
    }

    @GetMapping("/items/detail")
    public ApiResponse<TaobaoSearchResponse> detail(
            @RequestParam @NotBlank(message = "商品 ID 不能为空") @Size(max = 120, message = "不能超过 120 个字符") String itemId) {
        return ApiResponse.ok(taobaoAffiliateService.detail(itemId));
    }

    @PostMapping("/promotion-links")
    public ApiResponse<TaobaoPromotionResponse> promotion(@Valid @RequestBody TaobaoPromotionRequest request) {
        return ApiResponse.ok(taobaoAffiliateService.promotion(request));
    }

    @PostMapping("/imports")
    public ApiResponse<TaobaoImportResponse> importItem(@Valid @RequestBody TaobaoImportRequest request) {
        return ApiResponse.ok(taobaoAffiliateService.importItem(request));
    }

    @PostMapping("/refreshes")
    public ApiResponse<TaobaoImportResponse> refresh(@Valid @RequestBody TaobaoItemRequest request) {
        return ApiResponse.ok(taobaoAffiliateService.refresh(request));
    }

    @GetMapping("/jobs")
    public ApiResponse<PageResult<TaobaoJobResponse>> jobs(@Valid TaobaoJobQuery query) {
        return ApiResponse.ok(taobaoAffiliateService.jobs(query));
    }

    @PostMapping("/jobs/{id}/retry")
    public ApiResponse<TaobaoJobResponse> retry(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(taobaoAffiliateService.retry(id));
    }
}
