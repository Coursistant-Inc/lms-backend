package com.coursistant.lms.module.course.dto;

import com.coursistant.lms.module.assignment.entity.AssignmentFile;
import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.course.entity.CourseSchedule;

import java.util.List;

public class CourseDTO extends Course {

    private static final long serialVersionUID = 1L;

    private List<CourseSchedule> courseSchedules;



    public CourseDTO() {}

    public List<CourseSchedule> getCourseSchedules() {
        return courseSchedules;
    }

    public void setCourseSchedules(List<CourseSchedule> courseSchedules) {
        this.courseSchedules = courseSchedules;
    }
}
