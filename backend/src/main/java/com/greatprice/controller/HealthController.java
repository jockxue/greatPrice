package com.greatprice.controller;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.dto.HealthResponse;
import com.greatprice.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(healthService.check());
    }
}
