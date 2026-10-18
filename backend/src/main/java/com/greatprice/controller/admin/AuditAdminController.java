package com.greatprice.controller.admin;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.PageResult;
import com.greatprice.dto.admin.AuditLogResponse;
import com.greatprice.dto.admin.AuditQuery;
import com.greatprice.service.AuditQueryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/audit-logs")
public class AuditAdminController {

    private final AuditQueryService auditQueryService;

    public AuditAdminController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResult<AuditLogResponse>> page(@Valid AuditQuery query) {
        return ApiResponse.ok(auditQueryService.page(query));
    }
}
