package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.dto.CreateAssignmentRequest;
import com.coursistant.lms.v2.dto.CreateCourseRequest;
import com.coursistant.lms.v2.dto.CourseDetailV2DTO;
import com.coursistant.lms.v2.dto.CreateCourseUnitRequest;
import com.coursistant.lms.v2.entity.*;
import com.coursistant.lms.v2.repository.AssignmentRepository;
import com.coursistant.lms.v2.repository.CourseRepository;
import com.coursistant.lms.v2.repository.CourseUnitRepository;
import com.coursistant.lms.v2.repository.UserRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseV2Service {
    private final JPAQueryFactory queryFactory;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public CourseDetailV2DTO getCourseDetail(Long courseId) {
        CourseDetailV2DTO.CourseInfo courseInfo = queryCourseInfo(courseId);
        List<CourseDetailV2DTO.CourseUnit> courseUnits = queryCourseUnits(courseId);
        List<CourseDetailV2DTO.Assignment> assignments = queryAssignmentsForCourse(courseId);

        return new CourseDetailV2DTO(courseInfo, courseUnits, assignments);
    }

    private CourseDetailV2DTO.CourseInfo queryCourseInfo(Long courseId) {
        return queryFactory
                .select(Projections.constructor(
                        CourseDetailV2DTO.CourseInfo.class,
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

    private List<CourseDetailV2DTO.CourseUnit> queryCourseUnits(Long courseId) {
        return queryFactory
                .select(Projections.constructor(
                        CourseDetailV2DTO.CourseUnit.class,
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

    private List<CourseDetailV2DTO.Assignment> queryAssignmentsForCourse(Long courseId) {
        return queryFactory
                .select(Projections.constructor(
                        CourseDetailV2DTO.Assignment.class,
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

    @Transactional
    public CourseEntity createCourse(CreateCourseRequest course, Integer creatorId) {
        var newCourse = new CourseEntity();

        newCourse.setCourseCode(course.getCourseCode());
        newCourse.setName(course.getName());
        newCourse.setDescription(course.getDescription());
        newCourse.setSchool(course.getSchool());
        newCourse.setSemester(course.getSemester());

        var creator = userRepository.getReferenceById(creatorId);
        newCourse.setTeacher(creator);

        return courseRepository.save(newCourse);
    }

    @Transactional
    public CourseUnitEntity createCourseUnit(CreateCourseUnitRequest unit, Long courseId) {
        var newUnit = new CourseUnitEntity();

        newUnit.setSortOrder(unit.getSortOrder());
        newUnit.setTitle(unit.getTitle());
        newUnit.setDescription(unit.getDescription());

        var course = courseRepository.getReferenceById(courseId);
        newUnit.setCourse(course);

        return courseUnitRepository.save(newUnit);
    }

    @Transactional
    public AssignmentEntity createAssignment(CreateAssignmentRequest assignment, Long courseUnitId) {
        var newAssignment = new AssignmentEntity();

        newAssignment.setTitle(assignment.getTitle());
        newAssignment.setType(assignment.getType());
        newAssignment.setDueTime(assignment.getDueTime());

        var courseUnit = courseUnitRepository.getReferenceById(courseUnitId);
        newAssignment.setCourseUnit(courseUnit);

        return assignmentRepository.save(newAssignment);
    }

    private static final QCourseEntity course = QCourseEntity.courseEntity;
    private static final QCourseUnitEntity courseUnit = QCourseUnitEntity.courseUnitEntity;
    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
    private static final QUserEntity user = QUserEntity.userEntity;
}
