package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.assignment.repository.GradeCorrectionAuditMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * SYSTEM_ADMIN-only grade correction. Not for daily Instructor/TA grading.
 */
@RestController
@RequestMapping("/v2/system/grade-corrections")
@Tag(name = "SystemGradeCorrections", description = "SYSTEM_ADMIN audited grade corrections (not daily grading)")
public class SystemGradeCorrectionController {

    @Schema(name = "SystemGradeCorrectionRequest",
            description = "SYSTEM_ADMIN grade correction payload")
    public static class CorrectionRequest {
        @Schema(description = "Assignment id", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        public Integer assignmentId;
        @Schema(description = "Student user id whose grade is corrected", example = "385",
                requiredMode = Schema.RequiredMode.REQUIRED)
        public Integer studentUserId;
        @Schema(description = "Corrected score", example = "88.5", requiredMode = Schema.RequiredMode.REQUIRED)
        public BigDecimal score;
        @Schema(description = "Audit reason (required, non-blank)", example = "Regrade after appeal",
                requiredMode = Schema.RequiredMode.REQUIRED)
        public String reason;
    }

    @Resource
    private AuthzService authzService;
    @Resource
    private AssignmentMapper assignmentMapper;
    @Resource
    private AssignmentGradeMapper assignmentGradeMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private GradeCorrectionAuditMapper gradeCorrectionAuditMapper;

    @Idempotent
    @PostMapping("/assignments")
    @Transactional
    @Operation(
            operationId = "systemGradeCorrectionCreate",
            summary = "Correct an assignment grade (system admin)",
            description = "SYSTEM_ADMIN only. Updates an existing grade score and writes a correction audit row. "
                    + "Not for Instructor/TA daily grading."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING when assignmentId, studentUserId, score, or reason is missing",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "NOT_FOUND when assignment or grade row does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> correctAssignmentGrade(HttpServletRequest request, @RequestBody CorrectionRequest body) {
        authzService.requireSystemAdmin(request);
        if (body == null || body.assignmentId == null || body.studentUserId == null || body.score == null
                || body.reason == null || body.reason.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "assignmentId, studentUserId, score, reason required");
        }
        Assignment assignment = assignmentMapper.selectById(body.assignmentId);
        if (assignment == null) {
            throw new ApiException(ErrorType.NOT_FOUND);
        }
        Course course = courseMapper.selectById(assignment.getCourseId());
        AssignmentGrade existing = assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(body.assignmentId, body.studentUserId);
        String before = existing == null ? null : "{\"score\":\"" + existing.getScore() + "\"}";

        if (existing == null) {
            throw new ApiException(ErrorType.NOT_FOUND, "Grade not found");
        }
        existing.setScore(body.score);
        assignmentGradeMapper.updateById(existing);

        gradeCorrectionAuditMapper.insert(
                authzService.requireUserId(request),
                body.assignmentId,
                body.studentUserId,
                assignment.getCourseId(),
                course == null ? null : course.getTenantId(),
                body.reason.trim(),
                before,
                "{\"score\":\"" + body.score + "\"}");
        return ApiResponse.success();
    }
}
