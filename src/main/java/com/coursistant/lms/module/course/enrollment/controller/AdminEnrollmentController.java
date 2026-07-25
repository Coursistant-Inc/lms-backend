package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.AdminBatchEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.AdminEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.BatchEnrollResponse;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/admin/courses/{courseId}/enrollments")
public class AdminEnrollmentController {

    @Resource
    private EnrollmentService enrollmentService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Idempotent
    @PostMapping
    public ApiResponse<MemberResponse> enroll(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @RequestBody AdminEnrollRequest body) {
        requireAdmin(request);
        Integer adminId = currentUserId(request);
        if (body == null || body.getUserId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "userId is required");
        }
        return ApiResponse.success(enrollmentService.adminEnrollStudent(courseId, body.getUserId(), adminId));
    }

    @Idempotent
    @PostMapping("/batch")
    public ApiResponse<BatchEnrollResponse> enrollBatch(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @RequestBody AdminBatchEnrollRequest body) {
        requireAdmin(request);
        return ApiResponse.success(enrollmentService.adminBatchEnroll(courseId, body, currentUserId(request)));
    }

    @Idempotent
    @DeleteMapping("/{userId}")
    public ApiResponse<MemberResponse> deactivate(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer userId) {
        requireAdmin(request);
        return ApiResponse.success(
                enrollmentService.adminDeactivateEnrollment(courseId, userId, currentUserId(request)));
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!coursePermissionService.isAdmin(request)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Admin role required");
        }
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
