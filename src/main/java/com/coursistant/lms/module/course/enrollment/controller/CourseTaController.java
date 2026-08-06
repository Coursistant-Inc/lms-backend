package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.AdminEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/courses/{courseId}/tas")
public class CourseTaController {

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
        return ApiResponse.success(enrollmentMembershipService.addTa(
                actor, courseId, body.getUserId(), requestId(request)));
    }

    @Idempotent
    @PatchMapping("/{userId}/permissions")
    public ApiResponse<MemberResponse> patchPermissions(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer userId,
                                                        @RequestBody UpdateTaPermissionsRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(enrollmentMembershipService.patchTaPermissions(
                actor, courseId, userId, body, requestId(request)));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<MemberResponse> remove(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer userId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(enrollmentMembershipService.removeTa(
                actor, courseId, userId, requestId(request)));
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
