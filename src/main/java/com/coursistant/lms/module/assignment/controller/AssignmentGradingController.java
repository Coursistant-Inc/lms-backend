package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.GradeResponse;
import com.coursistant.lms.module.assignment.dto.GradeStudentSelectionRequest;
import com.coursistant.lms.module.assignment.dto.GradeTransitionResponse;
import com.coursistant.lms.module.assignment.dto.GradingRosterResponse;
import com.coursistant.lms.module.assignment.dto.GradingViewResponse;
import com.coursistant.lms.module.assignment.dto.UpsertGradeRequest;
import com.coursistant.lms.module.assignment.service.AssignmentGradingService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Grading endpoints for Instructors and TAs with the grade permission.
 *
 * <p>The roster is active Students only. Scores stay invisible to students until they are
 * released; retracting pulls them back without notifying anyone.</p>
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments/{assignmentId}")
public class AssignmentGradingController {

    @Resource
    private AssignmentGradingService assignmentGradingService;

    @GetMapping("/grading-roster")
    public ApiResponse<GradingRosterResponse> getRoster(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentGradingService.getRoster(courseId, assignmentId, currentUserId(request)));
    }

    @GetMapping("/students/{studentUserId}/grading")
    public ApiResponse<GradingViewResponse> getGradingView(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer assignmentId,
                                                           @PathVariable Integer studentUserId,
                                                           @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentGradingService.getGradingView(courseId, assignmentId, studentUserId,
                currentUserId(request), timezone));
    }

    @PutMapping("/students/{studentUserId}/grade")
    public ApiResponse<GradeResponse> upsertGrade(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer assignmentId,
                                                  @PathVariable Integer studentUserId,
                                                  @RequestBody UpsertGradeRequest body) {
        return ApiResponse.success(assignmentGradingService.upsertGrade(courseId, assignmentId, studentUserId,
                currentUserId(request), body));
    }

    @PostMapping(value = "/students/{studentUserId}/grade/annotated-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GradeResponse> uploadAnnotatedFile(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer assignmentId,
                                                          @PathVariable Integer studentUserId,
                                                          @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(assignmentGradingService.uploadAnnotatedFile(courseId, assignmentId, studentUserId,
                currentUserId(request), file));
    }

    @GetMapping("/students/{studentUserId}/grade/annotated-file")
    public ResponseEntity<InputStreamResource> downloadAnnotatedFile(HttpServletRequest request,
                                                                     @PathVariable Integer courseId,
                                                                     @PathVariable Integer assignmentId,
                                                                     @PathVariable Integer studentUserId) {
        return assignmentGradingService.downloadAnnotatedFile(courseId, assignmentId, studentUserId,
                currentUserId(request));
    }

    @Idempotent
    @PostMapping("/grades/release-all")
    public ApiResponse<GradeTransitionResponse> releaseAll(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentGradingService.releaseAll(courseId, assignmentId, currentUserId(request)));
    }

    @Idempotent
    @PostMapping("/grades/release")
    public ApiResponse<GradeTransitionResponse> release(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId,
                                                        @RequestBody GradeStudentSelectionRequest body) {
        return ApiResponse.success(assignmentGradingService.release(courseId, assignmentId, currentUserId(request),
                body == null ? null : body.getStudentUserIds()));
    }

    @Idempotent
    @PostMapping("/grades/retract")
    public ApiResponse<GradeTransitionResponse> retract(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId,
                                                        @RequestBody GradeStudentSelectionRequest body) {
        return ApiResponse.success(assignmentGradingService.retract(courseId, assignmentId, currentUserId(request),
                body == null ? null : body.getStudentUserIds()));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
