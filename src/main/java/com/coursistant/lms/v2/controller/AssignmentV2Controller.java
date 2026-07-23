package com.coursistant.lms.v2.controller;

import com.coursistant.lms.v2.common.ApiResponse;
import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.service.AssignmentV2Service;
import com.coursistant.lms.v2.service.FileV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;


@RestController
@RequestMapping("/v2/assignments")
@Slf4j
@RequiredArgsConstructor
public class AssignmentV2Controller {
    private final AssignmentV2Service assignmentService;
    private final FileV2Service fileService;


    @GetMapping("/{assignmentId}/edit")
    public ResponseEntity<ApiResponse<AssignmentForEditResponse>> getAssignmentForEdit(
            @PathVariable Long assignmentId
    ) {
        var assignment = assignmentService.getAssignmentForEdit(assignmentId);
        return ResponseEntity.ok(
                ApiResponse.success("Querying assignment success", assignment)
        );
    }

    @PostMapping("/{assignmentId}/edit")
    public ResponseEntity<ApiResponse<Boolean>> editAssignment(
            @PathVariable Long assignmentId,
            @RequestBody EditAssignmentRequest request
    ) {
        assignmentService.editAssignment(assignmentId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Editing assignment success", true)
        );
    }

    @PostMapping(value = "/{assignmentId}/edit/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload success, returns file id",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<ApiResponse<Long>> uploadAssignmentAttachment(
            @RequestAttribute("userId") Integer userId,
            @PathVariable Long assignmentId,
            @RequestPart("attachment") MultipartFile attachment
    ) {
        var fileId = assignmentService.uploadAttachment(assignmentId, attachment, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Editing assignment success", fileId)
        );
    }

    @PostMapping("/{assignmentId}/edit/attachments/{attachmentId}/delete")
    public ResponseEntity<ApiResponse<Boolean>> deleteAssignmentAttachment(
            @SuppressWarnings("unused") @PathVariable Long assignmentId,
            @PathVariable Long attachmentId
    ) {
        assignmentService.deleteAttachment(attachmentId);
        return ResponseEntity.ok(
                ApiResponse.success("Deleting attachment success", true)
        );
    }

    @GetMapping("/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<AssignmentForSubmissionResponse>> getAssignmentForSubmission(
            @RequestAttribute("userId") Integer userId,
            @PathVariable Long assignmentId
    ) {
        var result = assignmentService.getAssignmentForSubmission(assignmentId, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Querying assignment submission success", result)
        );
    }

    @PostMapping("/{assignmentId}/submission")
    public ResponseEntity<ApiResponse<Boolean>> submitAssignment(
            @RequestAttribute("userId") Integer userId,
            @PathVariable Long assignmentId,
            @RequestBody AssignmentSubmissionRequest request
    ) {
        assignmentService.createSubmission(assignmentId, userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Submitting assignment success", true)
        );
    }
    @PostMapping("/{assignmentId}/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<Boolean>> resubmitAssignment(
            @RequestAttribute("userId") Integer userId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestBody AssignmentSubmissionRequest request
    ) {
        assignmentService.resubmitSubmission(assignmentId, submissionId, userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Submitting assignment success", true)
        );
    }

    @PostMapping(value = "/{assignmentId}/submissions/{submissionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> uploadAssignmentSubmissionFile(
            @RequestAttribute("userId") Integer userId,
            @SuppressWarnings("unused") @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestPart("file") MultipartFile file
    ) {
        var result = assignmentService.uploadSubmissionFile(submissionId, file, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Uploading assignment submission file success", result)
        );
    }

    @PostMapping("/{assignmentId}/submissions/{submissionId}/files/{fileId}/delete")
    public ResponseEntity<ApiResponse<Boolean>> deleteAssignmentSubmissionFile(
            @SuppressWarnings("unused") @PathVariable Long assignmentId,
            @SuppressWarnings("unused") @PathVariable Long submissionId,
            @PathVariable Long fileId
    ) {
        assignmentService.deleteSubmissionFile(fileId);
        return ResponseEntity.ok(
                ApiResponse.success("Deleting submission file success", true)
        );
    }

    @GetMapping("/{courseId}/{assignmentId}/review")
    public ResponseEntity<ApiResponse<AssignmentForReviewResponse>> getAssignmentForReview(
            @PathVariable Long assignmentId,
            @PathVariable Integer courseId
    ) {
        var result = assignmentService.getAssignmentForReview(assignmentId, courseId);
        return ResponseEntity.ok(
                ApiResponse.success("Querying assignment review success", result)
        );
    }

    @PostMapping("/{assignmentId}/submissions/{submissionId}/review")
    public ResponseEntity<ApiResponse<Long>> createSubmissionReview(
            @SuppressWarnings("unused") @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestBody CreateSubmissionReviewRequest request
    ) {
        var result = assignmentService.createSubmissionReview(submissionId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating submission review success", result)
        );
    }

    @PostMapping("/{assignmentId}/submission/{submissionId}/reviews/updates")
    public ResponseEntity<ApiResponse<Boolean>> updateSubmissionReview(
            @SuppressWarnings("unused") @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestBody Map<Long, UpdateSubmissionReviewRequest> request
    ) {
        assignmentService.updateSubmissionReview(submissionId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Updating submission review success", true)
        );
    }

    @GetMapping("/download/assignmentFile")
    public ResponseEntity<Resource> downloadFile(@RequestParam Long fileId, HttpServletRequest request) {
        Resource file = fileService.downloadFile(fileId);

        String contentType = null;

        try {
                contentType = request.getServletContext()
                .getMimeType(file.getFile().getAbsolutePath());
        } catch(IOException ex) {
                contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);     
    }
    


}
