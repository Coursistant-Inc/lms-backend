package com.coursistant.lms.v2.controller;

import com.coursistant.lms.v2.common.ApiResponse;
import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.service.CourseV2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2/courses")
@Slf4j
@RequiredArgsConstructor
public class CourseV2Controller {
    private final CourseV2Service courseService;

    @GetMapping("/{courseId}/detail")
    public ResponseEntity<ApiResponse<CourseDetailV2DTO>> getCourseDetail(
            @PathVariable Long courseId
    ) {
        // TODO: Structured logging (already annotated with Slf4j)
        try {
            CourseDetailV2DTO detail = courseService.getCourseDetail(courseId);
            return ResponseEntity.ok(
                    ApiResponse.success("Querying course detail success", detail)
            );
        } catch (RuntimeException e) {
            // TODO: Domain exception encapsulation here
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "Course doesn't exist"));
        }
    }

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<Long>> createCourse(
            @RequestBody CreateCourseRequest request
    ) {
        // TODO: User id not implemented
        var course = courseService.createCourse(1, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating course success", course.getId())
        );
    }

    @PostMapping("/{courseId}/units/new")
    public ResponseEntity<ApiResponse<Long>> createCourseUnit(
            @PathVariable Long courseId,
            @RequestBody CreateCourseUnitRequest request
    ) {
        var unit = courseService.createCourseUnit(courseId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating course unit success", unit.getId())
        );
    }

    @PostMapping("/{courseId}/units/{courseUnitId}/assignments/new")
    public ResponseEntity<ApiResponse<Long>> createAssignment(
            @PathVariable Long ignoredCourseId,
            @PathVariable Long courseUnitId,
            @RequestBody CreateAssignmentRequest request
    ) {
        // TODO: Also validate relation here
        var assignment = courseService.createAssignment(courseUnitId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Creating assignment success", assignment.getId())
        );
    }

    @PostMapping("/{courseId}/update")
    public ResponseEntity<ApiResponse<Long>> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid UpdateCourseRequest request
    ) {
        courseService.updateCourse(courseId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Updating course success", courseId)
        );
    }
}
