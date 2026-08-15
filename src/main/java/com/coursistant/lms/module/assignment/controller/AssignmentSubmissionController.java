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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AssignmentSubmissions", description = "Student staging, submit, and submission file access")
public class AssignmentSubmissionController {

    @Resource
    private AssignmentSubmissionService assignmentSubmissionService;

    @PostMapping(value = "/submission-staging-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "assignmentUploadStagingFiles",
            summary = "Upload staging files (not yet submitted)",
            description = "Student only. Multipart parts: `files` (preferred, array) and/or single `file`. "
                    + "Uploading does not hand in — call POST /submissions afterward.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object"),
                            schemaProperties = {
                                    @SchemaProperty(
                                            name = "files",
                                            array = @ArraySchema(
                                                    schema = @Schema(type = "string", format = "binary"))),
                                    @SchemaProperty(
                                            name = "file",
                                            schema = @Schema(type = "string", format = "binary",
                                                    description = "Single-file fallback when `files` is omitted"))
                            }
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED (non-student), SUBMISSION_FROZEN",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "SUBMISSION_WINDOW_CLOSED, ASSIGNMENT_GROUP_SET_REQUIRED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING, SUBMISSION_FILE_COUNT_EXCEEDED, UNSUPPORTED_FILE_TYPE, FILE_TOO_LARGE",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
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
    @Operation(
            operationId = "assignmentListStagingFiles",
            summary = "List my staging files",
            description = "Student only. Lists files staged but not yet submitted."
    )
    public ApiResponse<List<StagingFileResponse>> listStagingFiles(HttpServletRequest request,
                                                                   @PathVariable Integer courseId,
                                                                   @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentSubmissionService.listStagingFiles(courseId, assignmentId,
                currentUserId(request)));
    }

    @DeleteMapping("/submission-staging-files/{stagingFileId}")
    @Operation(
            operationId = "assignmentDeleteStagingFile",
            summary = "Delete a staging file",
            description = "Student only. Removes one staged upload before submit."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or STAGING_FILE_INVALID / NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> deleteStagingFile(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer assignmentId,
                                               @PathVariable Integer stagingFileId) {
        assignmentSubmissionService.deleteStagingFile(courseId, assignmentId, stagingFileId, currentUserId(request));
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/submissions")
    @Operation(
            operationId = "assignmentSubmit",
            summary = "Submit staged files as a new version",
            description = "Student only. Consumes staging files (all active ones when stagingFileIds omitted) "
                    + "into an immutable submission version. Idempotent."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED, SUBMISSION_FROZEN",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "SUBMISSION_WINDOW_CLOSED, ASSIGNMENT_GROUP_SET_REQUIRED, NO_GROUP_MEMBERSHIP",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "STAGING_FILE_INVALID, SUBMISSION_FILE_COUNT_EXCEEDED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<SubmissionResponse> submit(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer assignmentId,
                                                  @RequestBody(required = false) SubmitAssignmentRequest body) {
        return ApiResponse.success(assignmentSubmissionService.submit(courseId, assignmentId, currentUserId(request),
                body), "Submitted");
    }

    @GetMapping("/submission")
    @Operation(
            operationId = "assignmentGetMySubmission",
            summary = "Get my submission state",
            description = "Student sees own submission; instructor/TA with grade permission may also read. "
                    + "Includes current version, staging files, and window flags."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<SubmissionResponse> getMySubmission(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentSubmissionService.getMySubmission(request, courseId, assignmentId,
                currentUserId(request)));
    }

    @GetMapping("/submissions/{submissionId}/versions")
    @Operation(
            operationId = "assignmentListSubmissionVersions",
            summary = "List submission versions",
            description = "Owner student or instructor/TA with grade access. Others receive ACCESS_DENIED."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or submission NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<List<SubmissionVersionResponse>> listVersions(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @PathVariable Integer submissionId) {
        return ApiResponse.success(assignmentSubmissionService.listVersions(request, courseId, assignmentId,
                submissionId, currentUserId(request)));
    }

    @GetMapping("/submissions/{submissionId}/files/{fileId}/download")
    @Operation(
            operationId = "assignmentDownloadSubmissionFile",
            summary = "Download a submission file",
            description = "Owner student or grading staff. Returns raw bytes with Content-Disposition: attachment."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Submission file bytes",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "attachment; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string",
                                            example = "attachment; filename=\"essay.pdf\"; filename*=UTF-8''essay.pdf")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Stored content type or application/octet-stream",
                                    schema = @Schema(type = "string"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or file NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> downloadSubmissionFile(HttpServletRequest request,
                                                                      @PathVariable Integer courseId,
                                                                      @PathVariable Integer assignmentId,
                                                                      @PathVariable Integer submissionId,
                                                                      @PathVariable Integer fileId) {
        return assignmentSubmissionService.streamSubmissionFile(request, courseId, assignmentId, submissionId, fileId,
                currentUserId(request), true);
    }

    @GetMapping("/submissions/{submissionId}/files/{fileId}/preview")
    @Operation(
            operationId = "assignmentPreviewSubmissionFile",
            summary = "Preview a submission file inline",
            description = "Owner student or grading staff. Returns raw bytes with Content-Disposition: inline "
                    + "(for browser preview when content type is previewable)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Submission file bytes for inline preview",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "inline; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string",
                                            example = "inline; filename=\"essay.pdf\"; filename*=UTF-8''essay.pdf")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Stored content type or application/octet-stream",
                                    schema = @Schema(type = "string"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "UNSUPPORTED_FILE_TYPE when the file is not previewable",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or file NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
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
