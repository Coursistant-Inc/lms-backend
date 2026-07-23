package com.coursistant.lms.v2.common;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import com.coursistant.lms.v2.entity.QAssignmentEntity;
import com.coursistant.lms.v2.entity.QCourseContentEntity;
import com.coursistant.lms.v2.entity.QSubmissionEntity;
import com.coursistant.lms.v2.entity.QCourseUnitContentEntity;
import com.coursistant.lms.v2.entity.QCourseUnitEntity;
import com.coursistant.lms.v2.entity.SubmissionEntity;
import com.coursistant.lms.v2.entity.CourseContentEntity;
import com.coursistant.lms.v2.entity.CourseUnitContentEntity;
import com.coursistant.lms.v2.entity.CourseUnitEntity;

import lombok.Getter;

@Getter
public enum EntityType {
    ASSIGNMENT("nw_assignment", QAssignmentEntity.class, AssignmentEntity.class),
    SUBMISSION("nw_submission", QSubmissionEntity.class, SubmissionEntity.class),
    COURSE("nw_course_content", QCourseContentEntity.class, CourseContentEntity.class),
    COURSEUNIT("nw_course_unit_content", QCourseUnitEntity.class, CourseUnitEntity.class);

    private final String code;
    private final Class<?> qClass;
    private final Class<?> entityClass;

    EntityType(String code, Class<?> qClass, Class<?> entityClass) {
        this.code = code;
        this.qClass = qClass;
        this.entityClass = entityClass;
    }
}