package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.Course;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CourseLifecycleSupportTest {

    @Test
    void gradingGrace_isArchivedAtPlus30Days() {
        Course course = new Course();
        course.setState("Archived");
        course.setArchivedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertEquals(LocalDateTime.of(2026, 1, 31, 0, 0), CourseLifecycleSupport.gradingGraceEndsAt(course));
        assertTrue(CourseLifecycleSupport.isWithinGradingGrace(course, LocalDateTime.of(2026, 1, 31, 0, 0)));
        assertFalse(CourseLifecycleSupport.isWithinGradingGrace(course, LocalDateTime.of(2026, 2, 1, 0, 0)));
        assertTrue(CourseLifecycleSupport.allowsGradingWrite(course, LocalDateTime.of(2026, 1, 15, 0, 0)));
        assertFalse(CourseLifecycleSupport.allowsGradingWrite(course, LocalDateTime.of(2026, 3, 1, 0, 0)));
    }

    @Test
    void activeCourse_allowsGradingWrite() {
        Course course = new Course();
        course.setState("Active");
        assertTrue(CourseLifecycleSupport.allowsGradingWrite(course, LocalDateTime.now()));
    }
}
