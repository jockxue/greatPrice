package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.dto.admin.AdminProfileResponse;
import com.greatprice.dto.admin.LoginRequest;
import com.greatprice.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/auth")
public class AuthController {

    private final AdminAuthService adminAuthService;

    public AuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/csrf")
    public ApiResponse<Void> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<AdminProfileResponse> login(@Valid @RequestBody LoginRequest request,
                                                   HttpServletRequest http,
                                                   HttpServletResponse response) {
        return ApiResponse.ok(adminAuthService.login(request, http, response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        adminAuthService.logout(request);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me() {
        return ApiResponse.ok(adminAuthService.me());
    }
}
