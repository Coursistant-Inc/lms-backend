package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.AdminBatchEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.AdminEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.BatchStudentEnrollResponse;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentMembershipService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/courses/{courseId}/students")
public class CourseStudentController {

    @Resource
    private EnrollmentMembershipService enrollmentMembershipService;
    @Resource
    private ActorContextResolver actorContextResolver;

    @Idempotent
    @PostMapping
    public ApiResponse<MemberResponse> add(HttpServletRequest request,
                                           @PathVariable Integer courseId,
                                           @RequestBody AdminEnrollRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        if (body == null || body.getUserId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "userId is required");
        }
        return ApiResponse.success(enrollmentMembershipService.addStudent(
                actor, courseId, body.getUserId(), requestId(request)));
    }

    @Idempotent
    @PostMapping("/batch")
    public ApiResponse<BatchStudentEnrollResponse> batch(HttpServletRequest request,
                                                         @PathVariable Integer courseId,
                                                         @RequestBody AdminBatchEnrollRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        if (body == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Request body is required");
        }
        return ApiResponse.success(enrollmentMembershipService.batchAddStudents(
                actor, courseId, body.getUserIds(), body.getEmails(), requestId(request)));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<MemberResponse> withdraw(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer userId) {
        ActorContext actor = actorContextResolver.resolve(request);
        MemberResponse resp = enrollmentMembershipService.withdrawStudent(
                actor, courseId, userId, requestId(request));
        return ApiResponse.success(resp);
    }

    private static String requestId(HttpServletRequest request) {
        String idem = request.getHeader("Idempotency-Key");
        if (idem != null && !idem.isBlank()) {
            return idem.trim();
        }
        String req = request.getHeader("X-Request-Id");
        return req == null || req.isBlank() ? null : req.trim();
    }
}
