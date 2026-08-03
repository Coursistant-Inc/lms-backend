package com.coursistant.lms.module.tenant.controller;

import com.coursistant.lms.module.tenant.dto.CreateTenantRequest;
import com.coursistant.lms.module.tenant.dto.PatchTenantRequest;
import com.coursistant.lms.module.tenant.dto.TenantResponse;
import com.coursistant.lms.module.tenant.service.TenantService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/admin/tenants")
public class AdminTenantController {

    @Resource
    private TenantService tenantService;

    @Resource
    private AuthzService authzService;

    @GetMapping
    public ApiResponse<List<TenantResponse>> list(HttpServletRequest request) {
        // Platform administrative: SYSTEM_ADMIN only
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(tenantService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> get(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(tenantService.getById(id));
    }

    @Idempotent
    @PostMapping
    public ApiResponse<TenantResponse> create(HttpServletRequest request, @RequestBody CreateTenantRequest body) {
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(tenantService.create(body));
    }

    @Idempotent
    @PatchMapping("/{id}")
    public ApiResponse<TenantResponse> patch(HttpServletRequest request,
                                             @PathVariable Integer id,
                                             @RequestBody PatchTenantRequest body) {
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(tenantService.patch(id, body));
    }

    @Idempotent
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        tenantService.delete(id);
        return ApiResponse.success();
    }
}
