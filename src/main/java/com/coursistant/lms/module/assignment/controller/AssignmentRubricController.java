package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.RubricResponse;
import com.coursistant.lms.module.assignment.service.AssignmentRubricService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
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
 * Rubric upload, read, download, and pointer rollback. Rubrics are PDF-only.
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments/{assignmentId}/rubric")
public class AssignmentRubricController {

    @Resource
    private AssignmentRubricService assignmentRubricService;

    @GetMapping
    public ApiResponse<RubricResponse> get(HttpServletRequest request,
                                           @PathVariable Integer courseId,
                                           @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentRubricService.get(request, courseId, assignmentId, currentUserId(request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId) {
        return assignmentRubricService.download(request, courseId, assignmentId, currentUserId(request));
    }

    @PostMapping("/restore-previous")
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
