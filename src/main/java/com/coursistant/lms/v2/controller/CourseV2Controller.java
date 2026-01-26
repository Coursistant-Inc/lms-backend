package com.coursistant.lms.v2.controller;

import com.coursistant.lms.v2.common.ApiResponse;
import com.coursistant.lms.v2.dto.CourseDetailV2DTO;
import com.coursistant.lms.v2.service.CourseV2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/courses")
@Slf4j
@RequiredArgsConstructor
public class CourseV2Controller {
    private final CourseV2Service courseService;

    @GetMapping("/{courseId}/detail")
    public ResponseEntity<ApiResponse<CourseDetailV2DTO>> getCourseDetail(@PathVariable Long courseId) {
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
}
