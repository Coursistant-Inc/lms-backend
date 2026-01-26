package com.coursistant.lms.v2.controller;

import com.coursistant.lms.v2.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/courses")
@Slf4j
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
}
