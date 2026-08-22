package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.RubricResponse;
import com.coursistant.lms.module.assignment.service.AssignmentRubricService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Rubric upload, read, download, preview, and pointer rollback. Rubrics are PDF-only.
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments/{assignmentId}/rubric")
@Tag(name = "AssignmentRubrics", description = "PDF rubric upload, download, preview, and version restore")
public class AssignmentRubricController {

    @Resource
    private AssignmentRubricService assignmentRubricService;

    @GetMapping
    @Operation(
            operationId = "assignmentGetRubric",
            summary = "Get current rubric pointer",
            description = "Course members with assignment visibility. Students only see Published assignment rubrics; "
                    + "posted=false when no rubric has been uploaded."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or RUBRIC_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<RubricResponse> get(HttpServletRequest request,
                                           @PathVariable Integer courseId,
                                           @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentRubricService.get(request, courseId, assignmentId, currentUserId(request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "assignmentUploadRubric",
            summary = "Upload a PDF rubric",
            description = "Instructor/TA. Multipart parts: required `file` (PDF), optional "
                    + "`confirmReplaceAfterGrading` when replacing after grading has started.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object", requiredProperties = {"file"}),
                            schemaProperties = {
                                    @SchemaProperty(
                                            name = "file",
                                            schema = @Schema(type = "string", format = "binary",
                                                    description = "PDF rubric file")),
                                    @SchemaProperty(
                                            name = "confirmReplaceAfterGrading",
                                            schema = @Schema(type = "boolean",
                                                    description = "Required true when grades already exist "
                                                            + "against a previous rubric"))
                            }
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "UNSUPPORTED_FILE_TYPE (non-PDF), FILE_TOO_LARGE",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "RUBRIC_REPLACE_CONFIRM_REQUIRED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<RubricResponse> upload(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer assignmentId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "confirmReplaceAfterGrading", required = false)
                                              Boolean confirmReplaceAfterGrading) {
        return ApiResponse.success(assignmentRubricService.upload(courseId, assignmentId, currentUserId(request),
                file, confirmReplaceAfterGrading));
    }

    @GetMapping("/download")
    @Operation(
            operationId = "assignmentDownloadRubric",
            summary = "Download current rubric PDF",
            description = "Course members with assignment visibility. Returns PDF bytes with "
                    + "Content-Disposition: attachment. Students cannot access Draft assignment rubrics."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rubric PDF bytes",
                    content = {
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "attachment; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string",
                                            example = "attachment; filename=\"rubric.pdf\"; filename*=UTF-8''rubric.pdf")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Typically application/pdf",
                                    schema = @Schema(type = "string", example = "application/pdf"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or RUBRIC_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId) {
        return assignmentRubricService.download(request, courseId, assignmentId, currentUserId(request));
    }

    @GetMapping("/preview")
    @Operation(
            operationId = "assignmentPreviewRubric",
            summary = "Preview current rubric PDF inline",
            description = "Course members with assignment visibility. Returns PDF bytes with "
                    + "Content-Disposition: inline. Students cannot access Draft assignment rubrics."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rubric PDF bytes for inline preview",
                    content = {
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "inline; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string",
                                            example = "inline; filename=\"rubric.pdf\"; filename*=UTF-8''rubric.pdf")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Typically application/pdf",
                                    schema = @Schema(type = "string", example = "application/pdf"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or RUBRIC_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer assignmentId) {
        return assignmentRubricService.preview(request, courseId, assignmentId, currentUserId(request));
    }

    @PostMapping("/restore-previous")
    @Operation(
            operationId = "assignmentRestorePreviousRubric",
            summary = "Restore previous rubric version",
            description = "Instructor/TA. Rolls the current pointer back one version. May require "
                    + "confirmReplaceAfterGrading when grading already occurred against the current rubric."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, RUBRIC_NOT_FOUND, RUBRIC_NO_PREVIOUS_VERSION",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "RUBRIC_REPLACE_CONFIRM_REQUIRED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<RubricResponse> restorePrevious(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer assignmentId,
                                                       @RequestParam(value = "confirmReplaceAfterGrading", required = false)
                                                       Boolean confirmReplaceAfterGrading) {
        return ApiResponse.success(assignmentRubricService.restorePrevious(courseId, assignmentId,
                currentUserId(request), confirmReplaceAfterGrading));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
