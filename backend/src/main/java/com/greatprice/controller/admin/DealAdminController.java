package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.admin.AdminDealQuery;
import com.greatprice.dto.admin.AdminDealResponse;
import com.greatprice.dto.admin.DealReviewResponse;
import com.greatprice.dto.admin.DealWriteRequest;
import com.greatprice.dto.admin.ReasonRequest;
import com.greatprice.dto.admin.VersionRequest;
import com.greatprice.service.DealAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/deals")
public class DealAdminController {

    private final DealAdminService dealAdminService;

    public DealAdminController(DealAdminService dealAdminService) {
        this.dealAdminService = dealAdminService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminDealResponse>> page(@Valid AdminDealQuery query) {
        return ApiResponse.ok(dealAdminService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminDealResponse> detail(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(dealAdminService.get(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<DealReviewResponse>> reviews(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(dealAdminService.reviews(id));
    }

    @PostMapping
    public ApiResponse<AdminDealResponse> create(@Valid @RequestBody DealWriteRequest request) {
        return ApiResponse.ok(dealAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminDealResponse> update(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                 @Valid @RequestBody DealWriteRequest request) {
        return ApiResponse.ok(dealAdminService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<AdminDealResponse> submit(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                 @Valid @RequestBody VersionRequest request) {
        return ApiResponse.ok(dealAdminService.submit(id, request.version()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDealResponse> approve(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                  @Valid @RequestBody VersionRequest request) {
        return ApiResponse.ok(dealAdminService.approve(id, request.version()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDealResponse> reject(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                 @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(dealAdminService.reject(id, request.version(), request.reason()));
    }

    @PostMapping("/{id}/offline")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDealResponse> offline(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                  @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(dealAdminService.offline(id, request.version(), request.reason()));
    }

    @PostMapping("/{id}/revise")
    public ApiResponse<AdminDealResponse> revise(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                 @Valid @RequestBody DealWriteRequest request) {
        return ApiResponse.ok(dealAdminService.revise(id, request));
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<AdminDealResponse> reopen(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                                 @Valid @RequestBody VersionRequest request) {
        return ApiResponse.ok(dealAdminService.reopen(id, request.version()));
    }
}
