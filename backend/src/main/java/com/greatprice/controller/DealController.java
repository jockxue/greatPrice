package com.greatprice.controller;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.DealDetailResponse;
import com.greatprice.dto.DealQueryRequest;
import com.greatprice.dto.DealSummaryResponse;
import com.greatprice.service.DealService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    /** 公开好价列表，支持分页、分类筛选、关键词搜索（商品名称 / 品牌 / 分类名称）和排序。 */
    @GetMapping
    public ApiResponse<PageResult<DealSummaryResponse>> list(@Valid DealQueryRequest query) {
        return ApiResponse.ok(dealService.listPublic(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<DealDetailResponse> detail(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(dealService.getPublicDetail(id));
    }
}
