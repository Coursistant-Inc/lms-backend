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
                                                           @PathVariable Integer studentUserId) {
        return ApiResponse.success(assignmentGradingService.getGradingView(courseId, assignmentId, studentUserId,
                currentUserId(request)));
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
        Integer userId = currentUserId(request);
        if (body != null && body.getGroupIds() != null && !body.getGroupIds().isEmpty()) {
            return ApiResponse.success(assignmentGradingService.releaseGroups(courseId, assignmentId, userId,
                    body.getGroupIds()));
        }
        return ApiResponse.success(assignmentGradingService.release(courseId, assignmentId, userId,
                body == null ? null : body.getStudentUserIds()));
    }

    @Idempotent
    @PostMapping("/grades/retract")
    public ApiResponse<GradeTransitionResponse> retract(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId,
                                                        @RequestBody GradeStudentSelectionRequest body) {
        Integer userId = currentUserId(request);
        if (body != null && body.getGroupIds() != null && !body.getGroupIds().isEmpty()) {
            return ApiResponse.success(assignmentGradingService.retractGroups(courseId, assignmentId, userId,
                    body.getGroupIds()));
        }
        return ApiResponse.success(assignmentGradingService.retract(courseId, assignmentId, userId,
                body == null ? null : body.getStudentUserIds()));
    }

    @GetMapping("/groups/{groupId}/grading")
    public ApiResponse<GradingViewResponse> getGroupGradingView(HttpServletRequest request,
                                                                @PathVariable Integer courseId,
                                                                @PathVariable Integer assignmentId,
                                                                @PathVariable Integer groupId) {
        return ApiResponse.success(assignmentGradingService.getGroupGradingView(courseId, assignmentId, groupId,
                currentUserId(request)));
    }

    @PutMapping("/groups/{groupId}/grade")
    public ApiResponse<GradeResponse> upsertGroupGrade(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer assignmentId,
                                                       @PathVariable Integer groupId,
                                                       @RequestBody UpsertGradeRequest body) {
        return ApiResponse.success(assignmentGradingService.upsertGroupGrade(courseId, assignmentId, groupId,
                currentUserId(request), body));
    }

    @PostMapping(value = "/groups/{groupId}/grade/annotated-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GradeResponse> uploadGroupAnnotatedFile(HttpServletRequest request,
                                                               @PathVariable Integer courseId,
                                                               @PathVariable Integer assignmentId,
                                                               @PathVariable Integer groupId,
                                                               @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(assignmentGradingService.uploadGroupAnnotatedFile(courseId, assignmentId, groupId,
                currentUserId(request), file));
    }

    @GetMapping("/groups/{groupId}/grade/annotated-file")
    public ResponseEntity<InputStreamResource> downloadGroupAnnotatedFile(HttpServletRequest request,
                                                                          @PathVariable Integer courseId,
                                                                          @PathVariable Integer assignmentId,
                                                                          @PathVariable Integer groupId) {
        return assignmentGradingService.downloadGroupAnnotatedFile(courseId, assignmentId, groupId,
                currentUserId(request));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
