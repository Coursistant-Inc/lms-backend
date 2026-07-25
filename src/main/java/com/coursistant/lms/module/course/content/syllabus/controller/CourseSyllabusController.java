package com.coursistant.lms.module.course.content.syllabus.controller;

import com.coursistant.lms.module.course.content.syllabus.dto.SyllabusResponse;
import com.coursistant.lms.module.course.content.syllabus.service.CourseSyllabusService;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v2/courses/{courseId}/syllabus")
public class CourseSyllabusController {

    @Resource
    private CourseSyllabusService courseSyllabusService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @GetMapping
    public ApiResponse<SyllabusResponse> get(HttpServletRequest request, @PathVariable Integer courseId) {
        boolean admin = coursePermissionService.isAdmin(request);
        return ApiResponse.success(courseSyllabusService.getSyllabus(courseId, currentUserId(request), admin));
    }

    @GetMapping("/preview")
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request, @PathVariable Integer courseId) {
        boolean admin = coursePermissionService.isAdmin(request);
        return courseSyllabusService.preview(courseId, currentUserId(request), admin);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request, @PathVariable Integer courseId) {
        boolean admin = coursePermissionService.isAdmin(request);
        return courseSyllabusService.download(courseId, currentUserId(request), admin);
    }

    @Idempotent
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SyllabusResponse> upload(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(courseSyllabusService.upload(courseId, currentUserId(request), file));
    }

    @Idempotent
    @PostMapping("/restore")
    public ApiResponse<SyllabusResponse> restore(HttpServletRequest request, @PathVariable Integer courseId) {
        return ApiResponse.success(courseSyllabusService.restorePrevious(courseId, currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
