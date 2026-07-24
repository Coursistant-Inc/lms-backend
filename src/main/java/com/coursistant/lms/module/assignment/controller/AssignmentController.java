package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.AssignmentAttachmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentSummaryResponse;
import com.coursistant.lms.module.assignment.dto.CreateAssignmentRequest;
import com.coursistant.lms.module.assignment.dto.DueDateChangePreviewRequest;
import com.coursistant.lms.module.assignment.dto.DueDateChangePreviewResponse;
import com.coursistant.lms.module.assignment.dto.PatchAssignmentRequest;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Assignment CRUD and instructor attachments.
 *
 * <p>Reads are role-shaped: students only ever see Published assignments and receive
 * {@code ASSIGNMENT_NOT_FOUND} for drafts. All write endpoints require an {@code X-Timezone}
 * header because dates arrive as wall-clock times.</p>
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments")
public class AssignmentController {

    @Resource
    private AssignmentService assignmentService;

    @GetMapping
    public ApiResponse<List<AssignmentResponse>> list(HttpServletRequest request,
                                                      @PathVariable Integer courseId,
                                                      @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentService.list(request, courseId, currentUserId(request), timezone));
    }

    /**
     * Slim list cards for any course member: title / due / type / submissionStatus.
     * Declared before {@code /{assignmentId}} so {@code summaries} is not parsed as an id.
     */
    @GetMapping("/summaries")
    public ApiResponse<List<AssignmentSummaryResponse>> listSummaries(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentService.listSummaries(request, courseId, currentUserId(request), timezone));
    }

    @GetMapping("/{assignmentId}")
    public ApiResponse<AssignmentResponse> detail(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer assignmentId,
                                                  @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentService.detail(request, courseId, assignmentId,
                currentUserId(request), timezone));
    }

    @PostMapping
    public ApiResponse<AssignmentResponse> create(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @RequestHeader(value = "X-Timezone", required = false) String timezone,
                                                  @RequestBody CreateAssignmentRequest body) {
        return ApiResponse.success(
                assignmentService.create(courseId, currentUserId(request), timezone, body),
                "Assignment created");
    }

    @PatchMapping("/{assignmentId}")
    public ApiResponse<AssignmentResponse> patch(HttpServletRequest request,
                                                 @PathVariable Integer courseId,
                                                 @PathVariable Integer assignmentId,
                                                 @RequestHeader(value = "X-Timezone", required = false) String timezone,
                                                 @RequestBody PatchAssignmentRequest body) {
        return ApiResponse.success(assignmentService.patch(courseId, assignmentId, currentUserId(request),
                timezone, body));
    }

    @PostMapping("/{assignmentId}/publish")
    public ApiResponse<AssignmentResponse> publish(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @PathVariable Integer assignmentId,
                                                   @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentService.publish(courseId, assignmentId, currentUserId(request), timezone));
    }

    @PostMapping("/{assignmentId}/unpublish")
    public ApiResponse<AssignmentResponse> unpublish(HttpServletRequest request,
                                                     @PathVariable Integer courseId,
                                                     @PathVariable Integer assignmentId,
                                                     @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentService.unpublish(courseId, assignmentId, currentUserId(request), timezone));
    }

    @DeleteMapping("/{assignmentId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer assignmentId) {
        assignmentService.delete(courseId, assignmentId, currentUserId(request));
        return ApiResponse.success();
    }

    /**
     * Dry run showing who is affected before a deadline is moved. Writes nothing.
     */
    @PostMapping("/{assignmentId}/due-date-change-preview")
    public ApiResponse<DueDateChangePreviewResponse> previewDueDateChange(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @RequestHeader(value = "X-Timezone", required = false) String timezone,
            @RequestBody DueDateChangePreviewRequest body) {
        return ApiResponse.success(assignmentService.previewDueDateChange(courseId, assignmentId,
                currentUserId(request), timezone, body));
    }

    @PostMapping(value = "/{assignmentId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<AssignmentAttachmentResponse>> uploadAttachments(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return ApiResponse.success(assignmentService.uploadAttachments(courseId, assignmentId,
                currentUserId(request), files));
    }

    @GetMapping("/{assignmentId}/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(HttpServletRequest request,
                                                                  @PathVariable Integer courseId,
                                                                  @PathVariable Integer assignmentId,
                                                                  @PathVariable Integer attachmentId) {
        return assignmentService.downloadAttachment(request, courseId, assignmentId, attachmentId,
                currentUserId(request));
    }

    @DeleteMapping("/{assignmentId}/attachments/{attachmentId}")
    public ApiResponse<Void> deleteAttachment(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer assignmentId,
                                              @PathVariable Integer attachmentId) {
        assignmentService.deleteAttachment(courseId, assignmentId, attachmentId, currentUserId(request));
        return ApiResponse.success();
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
