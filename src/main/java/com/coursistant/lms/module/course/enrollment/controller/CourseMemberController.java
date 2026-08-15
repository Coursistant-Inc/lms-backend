package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.MemberPageResponse;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentMembershipService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member list only. Promote/Demote TA routes removed (Part 3).
 * Student/TA mutations: {@code /students}, {@code /tas}.
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/members")
@Tag(name = "Enrollment", description = "Course membership: students, TAs, members, admin enroll")
public class CourseMemberController {

    @Resource
    private EnrollmentMembershipService enrollmentMembershipService;
    @Resource
    private ActorContextResolver actorContextResolver;

    @GetMapping
    @Operation(operationId = "courseMemberList", summary = "List course members with optional filters")
    public ApiResponse<MemberPageResponse> list(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @RequestParam(value = "courseRole", required = false) String courseRole,
                                                @RequestParam(value = "active", required = false) Boolean active,
                                                @RequestParam(value = "q", required = false) String q,
                                                @RequestParam(value = "page", required = false) Integer page,
                                                @RequestParam(value = "size", required = false) Integer size) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(enrollmentMembershipService.listMembers(
                actor, courseId, courseRole, active, q, page, size));
    }
}
