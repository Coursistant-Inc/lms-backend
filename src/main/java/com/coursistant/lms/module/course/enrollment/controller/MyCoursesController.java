package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.course.dto.MyCoursePageResponse;
import com.coursistant.lms.module.course.course.service.CourseService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/me/courses")
@Tag(name = "MeCourses", description = "Current-user enrolled courses")
public class MyCoursesController {

    @Resource
    private CourseService courseService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @GetMapping
    @Operation(operationId = "meCoursesList", summary = "List my enrolled courses")
    public ApiResponse<MyCoursePageResponse> list(HttpServletRequest request,
                                                  @RequestParam(value = "state", required = false) String state,
                                                  @RequestParam(value = "page", required = false) Integer page,
                                                  @RequestParam(value = "size", required = false) Integer size) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.listMyCourses(actor, state, page, size));
    }
}
