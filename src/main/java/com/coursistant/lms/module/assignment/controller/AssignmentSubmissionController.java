package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.StagingFileResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionVersionResponse;
import com.coursistant.lms.module.assignment.dto.SubmitAssignmentRequest;
import com.coursistant.lms.module.assignment.service.AssignmentSubmissionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * Student submission flow: stage files, submit them, and read back the resulting versions.
 *
 * <p>Staging and submitting are separate on purpose — uploading a file does not hand it in.</p>
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments/{assignmentId}")
public class AssignmentSubmissionController {

    @Resource
    private AssignmentSubmissionService assignmentSubmissionService;

    @PostMapping(value = "/submission-staging-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<StagingFileResponse>> uploadStagingFiles(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        MultipartFile[] resolved = resolveUploadFiles(files, file);
        return ApiResponse.success(assignmentSubmissionService.uploadStagingFiles(courseId, assignmentId,
                currentUserId(request), resolved), "Files staged; call POST /submissions to hand in");
    }

    @GetMapping("/submission-staging-files")
    public ApiResponse<List<StagingFileResponse>> listStagingFiles(HttpServletRequest request,
                                                                   @PathVariable Integer courseId,
                                                                   @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentSubmissionService.listStagingFiles(courseId, assignmentId,
                currentUserId(request)));
    }

    @DeleteMapping("/submission-staging-files/{stagingFileId}")
    public ApiResponse<Void> deleteStagingFile(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer assignmentId,
                                               @PathVariable Integer stagingFileId) {
        assignmentSubmissionService.deleteStagingFile(courseId, assignmentId, stagingFileId, currentUserId(request));
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/submissions")
    public ApiResponse<SubmissionResponse> submit(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer assignmentId,
                                                  @RequestHeader(value = "X-Timezone", required = false) String timezone,
                                                  @RequestBody(required = false) SubmitAssignmentRequest body) {
        return ApiResponse.success(assignmentSubmissionService.submit(courseId, assignmentId, currentUserId(request),
                timezone, body), "Submitted");
    }

    @GetMapping("/submission")
    public ApiResponse<SubmissionResponse> getMySubmission(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer assignmentId,
                                                           @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentSubmissionService.getMySubmission(request, courseId, assignmentId,
                currentUserId(request), timezone));
    }

    @GetMapping("/submissions/{submissionId}/versions")
    public ApiResponse<List<SubmissionVersionResponse>> listVersions(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @PathVariable Integer submissionId,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentSubmissionService.listVersions(request, courseId, assignmentId,
                submissionId, currentUserId(request), timezone));
    }

    @GetMapping("/submissions/{submissionId}/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadSubmissionFile(HttpServletRequest request,
                                                                      @PathVariable Integer courseId,
                                                                      @PathVariable Integer assignmentId,
                                                                      @PathVariable Integer submissionId,
                                                                      @PathVariable Integer fileId) {
        return assignmentSubmissionService.streamSubmissionFile(request, courseId, assignmentId, submissionId, fileId,
                currentUserId(request), true);
    }

    @GetMapping("/submissions/{submissionId}/files/{fileId}/preview")
    public ResponseEntity<InputStreamResource> previewSubmissionFile(HttpServletRequest request,
                                                                     @PathVariable Integer courseId,
                                                                     @PathVariable Integer assignmentId,
                                                                     @PathVariable Integer submissionId,
                                                                     @PathVariable Integer fileId) {
        return assignmentSubmissionService.streamSubmissionFile(request, courseId, assignmentId, submissionId, fileId,
                currentUserId(request), false);
    }

    private MultipartFile[] resolveUploadFiles(MultipartFile[] files, MultipartFile file) {
        if (files != null && files.length > 0) {
            return files;
        }
        if (file != null && !file.isEmpty()) {
            return new MultipartFile[]{file};
        }
        return files;
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
