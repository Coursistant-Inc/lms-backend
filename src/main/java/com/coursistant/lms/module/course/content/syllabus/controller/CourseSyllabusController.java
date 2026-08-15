package com.coursistant.lms.module.course.content.syllabus.controller;

import com.coursistant.lms.module.course.content.syllabus.dto.SyllabusResponse;
import com.coursistant.lms.module.course.content.syllabus.service.CourseSyllabusService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v2/courses/{courseId}/syllabus")
@Tag(name = "Syllabus", description = "Course syllabus metadata and file stream")
public class CourseSyllabusController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private CourseSyllabusService courseSyllabusService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @GetMapping
    @Operation(operationId = "courseSyllabusGet", summary = "Get syllabus metadata")
    public ApiResponse<SyllabusResponse> get(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.getSyllabus(actor, courseId));
    }

    @GetMapping("/preview")
    @Operation(
            operationId = "courseSyllabusPreview",
            summary = "Preview syllabus file (inline binary)",
            description = "Returns raw file bytes (not JSON ApiResponse). Content-Disposition is inline."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Syllabus file bytes for inline preview",
                    content = {
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/octet-stream",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = @Header(
                            name = "Content-Disposition",
                            description = "inline; filename=\"...\"",
                            schema = @Schema(type = "string", example = "inline; filename=\"syllabus.pdf\"")))
    })
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseSyllabusService.preview(actor, courseId);
    }

    @GetMapping("/download")
    @Operation(
            operationId = "courseSyllabusDownload",
            summary = "Download syllabus file (attachment binary)",
            description = "Returns raw file bytes (not JSON ApiResponse). Content-Disposition is attachment."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Syllabus file bytes for download",
                    content = {
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/octet-stream",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = @Header(
                            name = "Content-Disposition",
                            description = "attachment; filename=\"...\"",
                            schema = @Schema(type = "string", example = "attachment; filename=\"syllabus.pdf\"")))
    })
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseSyllabusService.download(actor, courseId);
    }

    @Idempotent
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "courseSyllabusUpload",
            summary = "Upload syllabus file",
            description = "Multipart field name must be `file`.",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string")),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object", requiredProperties = {"file"},
                                    description = "Multipart form with part `file`"))
            )
    )
    public ApiResponse<SyllabusResponse> upload(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @Parameter(description = "Syllabus file part", required = true,
                                                        content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                                                schema = @Schema(type = "string", format = "binary")))
                                                @RequestPart("file") MultipartFile file) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.upload(actor, courseId, file));
    }

    @Idempotent
    @PostMapping("/restore")
    @Operation(
            operationId = "courseSyllabusRestore",
            summary = "Restore previous syllabus version",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<SyllabusResponse> restore(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.restorePrevious(actor, courseId));
    }

    @DeleteMapping
    @Operation(operationId = "courseSyllabusClear", summary = "Clear current syllabus")
    public ApiResponse<SyllabusResponse> clear(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.clear(actor, courseId));
    }
}
