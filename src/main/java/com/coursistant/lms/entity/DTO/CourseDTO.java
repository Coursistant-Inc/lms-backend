package com.coursistant.lms.entity.DTO;

import com.coursistant.lms.entity.AssignmentFile;
import com.coursistant.lms.entity.Course;
import com.coursistant.lms.entity.CourseSchedule;

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
