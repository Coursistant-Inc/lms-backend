package com.coursistant.lms.module.tenant.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class TenantTimezoneService {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    public ZoneId requireZoneForTenant(Integer tenantId) {
        if (tenantId == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Persisted tenantId is null");
        }
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
        if (tenant.getTimezone() == null || tenant.getTimezone().isBlank()) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Tenant timezone is missing");
        }
        return TimeZoneUtils.resolveZoneId(tenant.getTimezone());
    }

    public ZoneId requireZoneForCourse(Integer courseId) {
        Course course = requireCourse(courseId);
        return requireZoneForTenant(course.getTenantId());
    }

    public ZoneId requireZoneForUser(Integer userId) {
        if (userId == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (user.getTenantId() == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Persisted user.tenantId is null");
        }
        return requireZoneForTenant(user.getTenantId());
    }

    public String requireTimezoneIdForCourse(Integer courseId) {
        return requireZoneForCourse(courseId).getId();
    }

    public String requireTimezoneIdForUser(Integer userId) {
        return requireZoneForUser(userId).getId();
    }

    public Integer requireUserTenantId(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (user.getTenantId() == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Persisted user.tenantId is null");
        }
        return user.getTenantId();
    }

    private Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        if (course.getTenantId() == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Persisted course.tenantId is null");
        }
        return course;
    }
}
