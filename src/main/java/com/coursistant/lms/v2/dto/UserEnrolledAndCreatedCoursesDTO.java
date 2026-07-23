package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.coursistant.lms.v2.entity.CourseEntity;
import com.coursistant.lms.v2.dto.CoursePreviewResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEnrolledAndCreatedCoursesDTO {

    private List<CourseEntity> enrolledCourses;
    private List<CourseEntity> createdCourses;

}
