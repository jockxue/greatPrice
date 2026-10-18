package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.admin.ProductQuery;
import com.greatprice.dto.admin.ProductResponse;
import com.greatprice.dto.admin.ProductWriteRequest;
import com.greatprice.service.ProductAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/products")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    public ProductAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping
    public ApiResponse<PageResult<ProductResponse>> page(@Valid ProductQuery query) {
        return ApiResponse.ok(productAdminService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> detail(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(productAdminService.get(id));
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductWriteRequest request) {
        return ApiResponse.ok(productAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable @Positive(message = "id 必须为正整数") Long id,
                                               @Valid @RequestBody ProductWriteRequest request) {
        return ApiResponse.ok(productAdminService.update(id, request));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<ProductResponse> disable(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(productAdminService.disable(id));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<ProductResponse> enable(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(productAdminService.enable(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        productAdminService.delete(id);
        return ApiResponse.ok();
    }
}
