package com.coursistant.lms.module.course.content.syllabus.controller;

import com.coursistant.lms.module.course.content.syllabus.dto.SyllabusResponse;
import com.coursistant.lms.module.course.content.syllabus.service.CourseSyllabusService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
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
public class CourseSyllabusController {

    @Resource
    private CourseSyllabusService courseSyllabusService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @GetMapping
    public ApiResponse<SyllabusResponse> get(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.getSyllabus(actor, courseId));
    }

    @GetMapping("/preview")
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseSyllabusService.preview(actor, courseId);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseSyllabusService.download(actor, courseId);
    }

    @Idempotent
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SyllabusResponse> upload(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @RequestPart("file") MultipartFile file) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.upload(actor, courseId, file));
    }

    @Idempotent
    @PostMapping("/restore")
    public ApiResponse<SyllabusResponse> restore(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.restorePrevious(actor, courseId));
    }

    @DeleteMapping
    public ApiResponse<SyllabusResponse> clear(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSyllabusService.clear(actor, courseId));
    }
}
