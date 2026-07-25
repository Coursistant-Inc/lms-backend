package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.dto.PromoteTaResponse;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
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
@RequestMapping("/v2/courses/{courseId}/members")
public class CourseMemberController {

    @Resource
    private EnrollmentService enrollmentService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @GetMapping
    public ApiResponse<List<MemberResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        coursePermissionService.requireInstructor(courseId, currentUserId(request));
        return ApiResponse.success(enrollmentService.listMembers(courseId));
    }

    @Idempotent
    @PostMapping("/{userId}/ta")
    public ApiResponse<PromoteTaResponse> promoteToTa(HttpServletRequest request,
                                                      @PathVariable Integer courseId,
                                                      @PathVariable Integer userId,
                                                      @RequestBody(required = false) UpdateTaPermissionsRequest body) {
        Integer actorId = currentUserId(request);
        coursePermissionService.requireInstructor(courseId, actorId);
        return ApiResponse.success(enrollmentService.promoteToTa(courseId, userId, body, actorId));
    }

    @DeleteMapping("/{userId}/ta")
    public ApiResponse<MemberResponse> revokeTa(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer userId) {
        Integer actorId = currentUserId(request);
        coursePermissionService.requireInstructor(courseId, actorId);
        return ApiResponse.success(enrollmentService.revokeTa(courseId, userId, actorId));
    }

    @Idempotent
    @PatchMapping("/{userId}/ta/permissions")
    public ApiResponse<MemberResponse> updateTaPermissions(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer userId,
                                                           @RequestBody UpdateTaPermissionsRequest body) {
        Integer actorId = currentUserId(request);
        coursePermissionService.requireInstructor(courseId, actorId);
        return ApiResponse.success(enrollmentService.updateTaPermissions(courseId, userId, body, actorId));
    }

    @Idempotent
    @DeleteMapping("/{userId}")
    public ApiResponse<MemberResponse> deactivateMember(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer userId) {
        Integer actorId = currentUserId(request);
        coursePermissionService.requireInstructor(courseId, actorId);
        return ApiResponse.success(enrollmentService.instructorDeactivateMember(courseId, userId, actorId));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
