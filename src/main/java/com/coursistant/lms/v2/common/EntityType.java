package com.coursistant.lms.v2.common;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import com.coursistant.lms.v2.entity.QAssignmentEntity;
import com.coursistant.lms.v2.entity.QSubmissionEntity;
import com.coursistant.lms.v2.entity.SubmissionEntity;
import lombok.Getter;

@Getter
public enum EntityType {
    ASSIGNMENT("nw_assignment", QAssignmentEntity.class, AssignmentEntity.class),
    SUBMISSION("nw_submission", QSubmissionEntity.class, SubmissionEntity.class);

    private final String code;
    private final Class<?> qClass;
    private final Class<?> entityClass;

    EntityType(String code, Class<?> qClass, Class<?> entityClass) {
        this.code = code;
        this.qClass = qClass;
        this.entityClass = entityClass;
    }
}