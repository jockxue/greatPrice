package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.dto.admin.CategoryAdminResponse;
import com.greatprice.service.CategoryAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    public CategoryAdminController(CategoryAdminService categoryAdminService) {
        this.categoryAdminService = categoryAdminService;
    }

    @GetMapping
    public ApiResponse<List<CategoryAdminResponse>> list() {
        return ApiResponse.ok(categoryAdminService.list());
    }
}
