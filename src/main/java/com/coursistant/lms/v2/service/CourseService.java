package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.dto.CourseDetailDTO;
import com.coursistant.lms.v2.entity.QAssignmentEntity;
import com.coursistant.lms.v2.entity.QCourseEntity;
import com.coursistant.lms.v2.entity.QCourseUnitEntity;
import com.coursistant.lms.v2.entity.QUserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final JPAQueryFactory queryFactory;

    public CourseDetailDTO getCourseDetail(Long courseId) {
        CourseDetailDTO.CourseInfo courseInfo = queryCourseInfo(courseId);
        List<CourseDetailDTO.CourseUnit> courseUnits = queryCourseUnits(courseId);
        List<CourseDetailDTO.Assignment> assignments = queryAssignmentsForCourse(courseId);

        return new CourseDetailDTO(courseInfo, courseUnits, assignments);
    }

    private CourseDetailDTO.CourseInfo queryCourseInfo(Long courseId) {
        return queryFactory
                .select(Projections.constructor(
                        CourseDetailDTO.CourseInfo.class,
                        course.id,
                        course.createdAt,
                        course.updatedAt,
                        course.courseCode,
                        course.name,
                        course.description,
                        course.school,
                        course.semester,
                        user.name.as("teacherName"),
                        user.phone.as("teacherPhone"),
                        user.email.as("teacherEmail")
                ))
                .from(course)
                .join(course.teacher, user)
                .where(course.id.eq(courseId))
                .fetchOne();
    }

    private List<CourseDetailDTO.CourseUnit> queryCourseUnits(Long courseId) {
        return queryFactory
                .select(Projections.constructor(
                        CourseDetailDTO.CourseUnit.class,
                        courseUnit.id,
                        courseUnit.createdAt,
                        courseUnit.updatedAt,
                        courseUnit.sortOrder,
                        courseUnit.title,
                        courseUnit.description
                ))
                .from(courseUnit)
                .where(courseUnit.course.id.eq(courseId))
                .orderBy(courseUnit.sortOrder.asc())
                .fetch();
    }

    private List<CourseDetailDTO.Assignment> queryAssignmentsForCourse(Long courseId) {
        return queryFactory
                .select(Projections.constructor(
                        CourseDetailDTO.Assignment.class,
                        assignment.id,
                        assignment.createdAt,
                        assignment.updatedAt,
                        courseUnit.id.as("courseUnitId"),
                        assignment.title,
                        assignment.type,
                        assignment.dueTime
                ))
                .from(assignment)
                .join(assignment.courseUnit, courseUnit)
                .where(courseUnit.course.id.eq(courseId))
                .fetch();
    }

    private static final QCourseEntity course = QCourseEntity.courseEntity;
    private static final QCourseUnitEntity courseUnit = QCourseUnitEntity.courseUnitEntity;
    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
    private static final QUserEntity user = QUserEntity.userEntity;
}
