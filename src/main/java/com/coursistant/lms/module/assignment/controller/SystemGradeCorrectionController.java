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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
public class SystemGradeCorrectionController {

    public static class CorrectionRequest {
        public Integer assignmentId;
        public Integer studentUserId;
        public BigDecimal score;
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
