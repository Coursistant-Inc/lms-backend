package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.AdminBatchEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.AdminEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.BatchEnrollResponse;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
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
    private AuthzService authzService;

    @Idempotent
    @PostMapping
    public ApiResponse<MemberResponse> enroll(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @RequestBody AdminEnrollRequest body) {
        // Platform administrative enrollment
        authzService.requireSystemAdmin(request);
        Integer adminId = authzService.requireUserId(request);
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
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(enrollmentService.adminBatchEnroll(courseId, body, authzService.requireUserId(request)));
    }

    @Idempotent
    @DeleteMapping("/{userId}")
    public ApiResponse<MemberResponse> deactivate(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer userId) {
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(
                enrollmentService.adminDeactivateEnrollment(courseId, userId, authzService.requireUserId(request)));
    }
}
