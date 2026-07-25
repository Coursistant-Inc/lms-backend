package com.coursistant.lms.module.course.content.week.controller;

import com.coursistant.lms.module.course.content.week.dto.CreateWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.RenameWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.ReorderWeeksRequest;
import com.coursistant.lms.module.course.content.week.dto.WeekResponse;
import com.coursistant.lms.module.course.content.week.service.CourseWeekService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/weeks")
public class CourseWeekController {

    @Resource
    private CourseWeekService courseWeekService;

    @GetMapping
    public ApiResponse<List<WeekResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        return ApiResponse.success(courseWeekService.list(request, courseId, currentUserId(request)));
    }

    @Idempotent
    @PostMapping
    public ApiResponse<WeekResponse> create(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @RequestBody CreateWeekRequest body) {
        return ApiResponse.success(courseWeekService.create(courseId, currentUserId(request), body));
    }

    @Idempotent
    @PatchMapping("/{weekId}")
    public ApiResponse<WeekResponse> rename(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer weekId,
                                            @RequestBody RenameWeekRequest body) {
        return ApiResponse.success(courseWeekService.rename(courseId, weekId, currentUserId(request), body));
    }

    @Idempotent
    @PutMapping("/reorder")
    public ApiResponse<List<WeekResponse>> reorder(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @RequestBody ReorderWeeksRequest body) {
        return ApiResponse.success(courseWeekService.reorder(courseId, currentUserId(request), body));
    }

    @Idempotent
    @PostMapping("/{weekId}/publish")
    public ApiResponse<WeekResponse> publish(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer weekId) {
        return ApiResponse.success(courseWeekService.publish(courseId, weekId, currentUserId(request)));
    }

    @Idempotent
    @PostMapping("/{weekId}/unpublish")
    public ApiResponse<WeekResponse> unpublish(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer weekId) {
        return ApiResponse.success(courseWeekService.unpublish(courseId, weekId, currentUserId(request)));
    }

    @DeleteMapping("/{weekId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer weekId) {
        courseWeekService.delete(courseId, weekId, currentUserId(request));
        return ApiResponse.success();
    }

    @GetMapping("/{weekId}/download.zip")
    public ResponseEntity<StreamingResponseBody> downloadZip(HttpServletRequest request,
                                                             @PathVariable Integer courseId,
                                                             @PathVariable Integer weekId) {
        return courseWeekService.downloadZip(request, courseId, weekId, currentUserId(request));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
