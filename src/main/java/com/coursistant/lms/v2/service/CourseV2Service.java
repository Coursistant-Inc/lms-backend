package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.entity.*;
import com.coursistant.lms.v2.repository.AssignmentRepository;
import com.coursistant.lms.v2.repository.CourseRepository;
import com.coursistant.lms.v2.repository.CourseUnitRepository;
import com.coursistant.lms.v2.repository.UserRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseV2Service {
    private final JPAQueryFactory queryFactory;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseUnitRepository courseUnitRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<CoursePreviewResponse> getCourses() {
        return queryFactory
                .select(Projections.constructor(CoursePreviewResponse.class,
                        course.id,
                        course.courseCode,
                        course.name,
                        course.teacher.name,
                        courseUnit.id.count().intValue(),
                        Expressions.nullExpression()
                ))
                .from(course)
                .innerJoin(user).on(course.teacher.id.eq(user.id))
                .leftJoin(courseUnit).on(course.id.eq(courseUnit.course.id))
                .groupBy(course.id)
                .fetch();
    }

    @Transactional(readOnly = true)
    public CourseDetailV2DTO getCourseDetail(Long courseId) {
        var courseInfo = queryCourseInfo(courseId);
        var courseUnits = queryCourseUnits(courseId);
        var assignments = queryAssignmentsForCourse(courseId);

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
    public CourseEntity createCourse(Integer creatorId, CreateCourseRequest course) {
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
    public CourseUnitEntity createCourseUnit(Long courseId, CreateCourseUnitRequest unit) {
        var newUnit = new CourseUnitEntity();

        newUnit.setSortOrder(unit.getSortOrder());
        newUnit.setTitle(unit.getTitle());
        newUnit.setDescription(unit.getDescription());

        var course = courseRepository.getReferenceById(courseId);
        newUnit.setCourse(course);

        return courseUnitRepository.save(newUnit);
    }

    @Transactional
    public AssignmentEntity createAssignment(Long courseUnitId, CreateAssignmentRequest assignment) {
        var newAssignment = new AssignmentEntity();

        newAssignment.setTitle(assignment.getTitle());
        newAssignment.setDescription("");
        newAssignment.setType(assignment.getType());
        newAssignment.setDueTime(assignment.getDueTime());
        newAssignment.setSettings(new AssignmentEntity.AssignmentSettings(false, 100));

        var courseUnit = courseUnitRepository.getReferenceById(courseUnitId);
        newAssignment.setCourseUnit(courseUnit);

        return assignmentRepository.save(newAssignment);
    }

    @Transactional
    public void updateCourse(Long courseId, UpdateCourseRequest update) {
        if (!update.hasUpdates()) return;

        if (update.courseUpdate() != null) updateCourseEntity(courseId, update.courseUpdate());
        if (!CollectionUtils.isEmpty(update.courseUnitUpdateMap())) {
            batchUpdateCourseUnits(courseId, update.courseUnitUpdateMap());
        }
        if (!CollectionUtils.isEmpty(update.assignmentUpdateMap())) {
            batchUpdateAssignments(courseId, update.assignmentUpdateMap());
        }
    }

    private void updateCourseEntity(Long courseId, UpdateCourseRequest.CourseUpdate update) {
        if (!update.hasUpdates()) return;

        var clause = queryFactory.update(course);

        if (update.courseCode() != null) clause.set(course.courseCode, update.courseCode());
        if (update.name() != null) clause.set(course.name, update.name());
        if (update.description() != null) clause.set(course.description, update.description());
        if (update.school() != null) clause.set(course.school, update.school());
        if (update.semester() != null) clause.set(course.semester, update.semester());

        clause.where(course.id.eq(courseId)).execute();
    }

    private void batchUpdateCourseUnits(Long ignoredCourseId, Map<Long, UpdateCourseRequest.CourseUnitUpdate> updates) {
        // TODO: validation for correct relations

        for (var entry : updates.entrySet()) {
            var unitId = entry.getKey();
            var update = entry.getValue();
            if (!update.hasUpdates()) continue;

            var clause = queryFactory.update(courseUnit);

            if (update.sortOrder() != null) clause.set(courseUnit.sortOrder, update.sortOrder());
            if (update.title() != null) clause.set(courseUnit.title, update.title());
            if (update.description() != null) clause.set(courseUnit.description, update.description());

            clause.where(courseUnit.id.eq(unitId)).execute();
        }
    }

    private void batchUpdateAssignments(Long ignoredCourseId, Map<Long, UpdateCourseRequest.AssignmentUpdate> updates) {
        for (var entry : updates.entrySet()) {
            var assignmentId = entry.getKey();
            var update = entry.getValue();
            if (!update.hasUpdates()) continue;

            var clause = queryFactory.update(assignment);

            if (update.title() != null) clause.set(assignment.title, update.title());
            if (update.description() != null) clause.set(assignment.description, update.description());
            if (update.type() != null) clause.set(assignment.type, update.type());
            if (update.dueTime() != null) clause.set(assignment.dueTime, update.dueTime());
            if (update.settings() != null) clause.set(assignment.settings, update.settings());

            clause.where(assignment.id.eq(assignmentId)).execute();
        }
    }

    public void deleteCourse(Long courseId) {
        try {
            courseRepository.deleteById(courseId);
        } catch (EmptyResultDataAccessException e) {
            log.info("Course with id {} not found", courseId);
        }
    }

    public void deleteCourseUnit(Long courseUnitId) {
        try {
            courseUnitRepository.deleteById(courseUnitId);
        } catch (EmptyResultDataAccessException e) {
            log.info("Course unit with id {} not found", courseUnitId);
        }
    }

    public void deleteAssignment(Long assignmentId) {
        try {
            assignmentRepository.deleteById(assignmentId);
        } catch (EmptyResultDataAccessException e) {
            log.info("Assignment with id {} not found", assignmentId);
        }
    }

    private static final QCourseEntity course = QCourseEntity.courseEntity;
    private static final QCourseUnitEntity courseUnit = QCourseUnitEntity.courseUnitEntity;
    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
    private static final QUserEntity user = QUserEntity.userEntity;
}
