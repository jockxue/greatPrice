package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.dto.admin.AdminUserResponse;
import com.greatprice.dto.admin.UserCreateRequest;
import com.greatprice.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(adminUserService.list());
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(adminUserService.create(request));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<AdminUserResponse> disable(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(adminUserService.disable(id));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<AdminUserResponse> enable(@PathVariable @Positive(message = "id 必须为正整数") Long id) {
        return ApiResponse.ok(adminUserService.enable(id));
    }
}
