package com.coursistant.lms.module.course.content.week.controller;

import com.coursistant.lms.module.course.content.week.dto.CreateWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.RenameWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.ReorderWeeksRequest;
import com.coursistant.lms.module.course.content.week.dto.WeekResponse;
import com.coursistant.lms.module.course.content.week.service.CourseWeekService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/weeks")
public class CourseWeekController {

    @Resource
    private CourseWeekService courseWeekService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @GetMapping
    public ApiResponse<List<WeekResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.list(actor, courseId));
    }

    @Idempotent
    @PostMapping
    public ApiResponse<WeekResponse> create(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @RequestBody CreateWeekRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.create(actor, courseId, body));
    }

    @Idempotent
    @PatchMapping("/{weekId}")
    public ApiResponse<WeekResponse> rename(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer weekId,
                                            @RequestBody RenameWeekRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.rename(actor, courseId, weekId, body));
    }

    @Idempotent
    @PutMapping("/reorder")
    public ApiResponse<List<WeekResponse>> reorder(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @RequestBody ReorderWeeksRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.reorder(actor, courseId, body));
    }

    @Idempotent
    @PostMapping("/{weekId}/publish")
    public ApiResponse<WeekResponse> publish(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.publish(actor, courseId, weekId));
    }

    @Idempotent
    @PostMapping("/{weekId}/unpublish")
    public ApiResponse<WeekResponse> unpublish(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.unpublish(actor, courseId, weekId));
    }

    @DeleteMapping("/{weekId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseWeekService.delete(actor, courseId, weekId);
        return ApiResponse.success();
    }

    @GetMapping("/{weekId}/download.zip")
    public ResponseEntity<StreamingResponseBody> downloadZip(HttpServletRequest request,
                                                             @PathVariable Integer courseId,
                                                             @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseWeekService.downloadZip(actor, courseId, weekId);
    }
}
