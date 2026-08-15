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
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy admin enrollment paths; delegates to Part 3 membership service.
 * Prefer {@code /v2/courses/{id}/students}.
 */
@RestController
@RequestMapping("/v2/admin/courses/{courseId}/enrollments")
@Tag(name = "Enrollment", description = "Course membership: students, TAs, members, admin enroll")
public class AdminEnrollmentController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private EnrollmentMembershipService enrollmentMembershipService;
    @Resource
    private ActorContextResolver actorContextResolver;
    @Resource
    private AuthzService authzService;

    @Idempotent
    @PostMapping
    @Operation(
            operationId = "adminEnrollmentEnroll",
            summary = "System-admin enroll a student (legacy path)",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<MemberResponse> enroll(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @RequestBody AdminEnrollRequest body) {
        authzService.requireSystemAdmin(request);
        ActorContext actor = actorContextResolver.resolve(request);
        if (body == null || body.getUserId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "userId is required");
        }
        return ApiResponse.success(enrollmentMembershipService.addStudent(
                actor, courseId, body.getUserId(), requestId(request)));
    }

    @Idempotent
    @PostMapping("/batch")
    @Operation(
            operationId = "adminEnrollmentEnrollBatch",
            summary = "System-admin batch enroll students (legacy path)",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<BatchStudentEnrollResponse> enrollBatch(HttpServletRequest request,
                                                               @PathVariable Integer courseId,
                                                               @RequestBody AdminBatchEnrollRequest body) {
        authzService.requireSystemAdmin(request);
        ActorContext actor = actorContextResolver.resolve(request);
        if (body == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Request body is required");
        }
        return ApiResponse.success(enrollmentMembershipService.batchAddStudents(
                actor, courseId, body.getUserIds(), body.getEmails(), requestId(request)));
    }

    @DeleteMapping("/{userId}")
    @Operation(operationId = "adminEnrollmentDeactivate", summary = "System-admin withdraw a student (legacy path)")
    public ApiResponse<MemberResponse> deactivate(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer userId) {
        authzService.requireSystemAdmin(request);
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(enrollmentMembershipService.withdrawStudent(
                actor, courseId, userId, requestId(request)));
    }

    private static String requestId(HttpServletRequest request) {
        String idem = request.getHeader("Idempotency-Key");
        if (idem != null && !idem.isBlank()) {
            return idem.trim();
        }
        return null;
    }
}
