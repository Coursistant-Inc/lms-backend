package com.coursistant.lms.module.tenant.service;

import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.tenant.dto.CreateTenantRequest;
import com.coursistant.lms.module.tenant.dto.PatchTenantRequest;
import com.coursistant.lms.module.tenant.dto.TenantResponse;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantService {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private CourseMapper courseMapper;

    public List<TenantResponse> list() {
        return tenantMapper.selectAllOrderByNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TenantResponse getById(Integer id) {
        return toResponse(requireTenant(id));
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        if (request == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Request body is required");
        }
        String name = requireTrimmedName(request.getName());
        String timezone = requireValidTimezone(request.getTimezone());
        ensureNameAvailable(name, null);

        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setTimezone(timezone);
        try {
            tenantMapper.insert(tenant);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorType.TENANT_NAME_EXISTS);
        }
        return toResponse(requireTenant(tenant.getId()));
    }

    @Transactional
    public TenantResponse patch(Integer id, PatchTenantRequest request) {
        Tenant existing = requireTenant(id);
        if (request == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Request body is required");
        }

        boolean changed = false;
        Tenant update = new Tenant();
        update.setId(id);

        if (request.getName() != null) {
            String name = requireTrimmedName(request.getName());
            ensureNameAvailable(name, id);
            update.setName(name);
            changed = true;
        }
        if (request.getTimezone() != null) {
            update.setTimezone(requireValidTimezone(request.getTimezone()));
            changed = true;
        }
        if (!changed) {
            return toResponse(existing);
        }

        try {
            tenantMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorType.TENANT_NAME_EXISTS);
        }
        return toResponse(requireTenant(id));
    }

    @Transactional
    public void delete(Integer id) {
        requireTenant(id);
        long courseCount = courseMapper.countByTenantId(id);
        if (courseCount > 0) {
            throw new ApiException(ErrorType.TENANT_IN_USE);
        }
        tenantMapper.deleteById(id);
    }

    private Tenant requireTenant(Integer id) {
        if (id == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
        Tenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    private void ensureNameAvailable(String name, Integer excludeId) {
        Tenant other = tenantMapper.selectByName(name);
        if (other != null && (excludeId == null || !excludeId.equals(other.getId()))) {
            throw new ApiException(ErrorType.TENANT_NAME_EXISTS);
        }
    }

    private String requireTrimmedName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "name is required");
        }
        return name.trim();
    }

    private String requireValidTimezone(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE, "timezone is required");
        }
        String trimmed = timezone.trim();
        try {
            return ZoneId.of(trimmed).getId();
        } catch (DateTimeException e) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE, "Invalid IANA timezone");
        }
    }

    private TenantResponse toResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setTimezone(tenant.getTimezone());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }
}
