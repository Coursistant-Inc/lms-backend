package com.coursistant.lms.v2.service;

import com.coursistant.lms.v2.dto.*;
import com.coursistant.lms.v2.entity.*;
import com.coursistant.lms.v2.common.EntityType;
import com.coursistant.lms.v2.service.FileV2Service;
import com.coursistant.lms.v2.repository.*;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import com.coursistant.lms.v2.common.EntityType;

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
    private final UserCourseRelationRepository userCourseRelationRepository;
    private final FileReferenceRepository fileReferenceRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseContentRepository courseContentRepository;
    private final CourseUnitContentRepository courseUnitContentRepository;
    private final FileV2Service fileService;

    @Transactional(readOnly = true)
    public List<CoursePreviewResponse> getCourses(Integer userId) {
        return queryFactory
                .select(Projections.constructor(CoursePreviewResponse.class,
                        course.id,
                        course.courseCode,
                        course.name,
                        course.teacher.name,
                        courseUnit.id.count().intValue(),
                        Expressions.nullExpression(String.class)
                ))
                .from(course)
                .innerJoin(userCourseRelation).on(userCourseRelation.course.id.eq(course.id)
                        .and(userCourseRelation.user.id.eq(userId)))
                .leftJoin(courseUnit).on(course.id.eq(courseUnit.course.id))
                .groupBy(course.id)
                .fetch();
    }

    @Transactional(readOnly = true)
    public CourseDetailV2DTO getCourseDetail(Long courseId, Integer studentId) {
        var courseInfo = queryCourseInfo(courseId);
        var courseUnits = queryCourseUnits(courseId);
        var assignments = queryAssignmentsForCourse(courseId, studentId);
        var courseFiles = fileService.getFileReferencesByEntity(EntityType.COURSE, courseId);

        return new CourseDetailV2DTO(courseInfo, courseUnits, assignments, courseFiles);
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

private List<CourseDetailV2DTO.Assignment> queryAssignmentsForCourse(Long courseId, Integer studentId) {
    QSubmissionEntity submission = QSubmissionEntity.submissionEntity;
    

    try {
        List<CourseDetailV2DTO.Assignment> result = queryFactory
                .select(Projections.constructor(
                        CourseDetailV2DTO.Assignment.class,
                        assignment.id,
                        assignment.createdAt,
                        assignment.updatedAt,
                        courseUnit.id.as("courseUnitId"),
                        assignment.title,
                        assignment.type,
                        assignment.dueTime,
                        assignment.settings,
                        assignment.gradePublish,
                        assignment.submissionRequired,
                        assignment.isGroup,
                        submission.submissionCount
                ))
                .from(assignment)
                .join(assignment.courseUnit, courseUnit)
                .leftJoin(submission)
                    .on(submission.assignment.id.eq(assignment.id)
                        .and(submission.student.id.eq(studentId)))
                .where(courseUnit.course.id.eq(courseId))
                .fetch();
        
        return result;
        
    } catch (Exception e) {
        System.err.println("Query failed with error: " + e.getMessage());
        e.printStackTrace();
        throw e;
    }
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

        var result = courseRepository.save(newCourse);

        var relation = UserCourseRelationEntity.builder().user(creator).course(result).build();
        userCourseRelationRepository.save(relation);

        return result;
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
        newAssignment.setDescription(assignment.getDescription());
        newAssignment.setType(assignment.getType());
        newAssignment.setDueTime(assignment.getDueTime());
        // newAssignment.setSettings(new AssignmentEntity.AssignmentSettings(false, 100));
        newAssignment.setSettings(assignment.getSettings());
        newAssignment.setIsGroup(assignment.getIsGroup());
        newAssignment.setSubmissionRequired(assignment.getSubmissionRequired());

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

    @Transactional
    public void batchUpdateCourseUnits(Long ignoredCourseId, Map<Long, UpdateCourseRequest.CourseUnitUpdate> updates) {
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

    @Transactional
    public void deleteCourse(Long courseId) {
        try {
            List<FileReferenceEntity> files = fileReferenceRepository.findByEntityId(courseId);
            files.forEach(file -> fileService.deleteFile(file.getId()));
            fileReferenceRepository.deleteByEntityId(courseId);
            courseRepository.deleteById(courseId);
        } catch (EmptyResultDataAccessException e) {
            log.info("Course with id {} not found", courseId);
        }
    }

    public void deleteCourseUnit(Long courseUnitId) {
        try {

            List<FileReferenceEntity> files = fileReferenceRepository.findByEntityId(courseUnitId);
            files.forEach(file -> fileService.deleteFile(file.getId()));
            fileReferenceRepository.deleteByEntityId(courseUnitId);
            courseUnitRepository.deleteById(courseUnitId);
        } catch (EmptyResultDataAccessException e) {
            log.info("Course unit with id {} not found", courseUnitId);
        }
    }

    @Transactional
    public void deleteAssignment(Long assignmentId) {
        try {
            List<FileReferenceEntity> files = fileReferenceRepository.findByEntityId(assignmentId);
            files.forEach(file -> fileService.deleteFile(file.getId()));
            assignmentRepository.deleteById(assignmentId);
            fileReferenceRepository.deleteByEntityId(assignmentId);

        } catch (EmptyResultDataAccessException e) {
            log.info("Assignment with id {} not found", assignmentId);
        }

        // try {
        //     fileService.deleteFile(assignmentId);
        // } catch (Exception e){
        //     log.error("Exception found: "+e.getMessage());
        // }

        // try {
        //         fileReferenceRepository.deleteByEntityId(assignmentId);
        // }

        // catch(Exception e){
        //     log.error("Exception found: "+e.getMessage());
        // }

    }

    private List<CourseEntity> getEnrolledCourses(Integer studentId) {
        try {
            List<CourseEntity> enrolledCourses = queryFactory.selectFrom(course).
            join(learn).on(learn.courseId.id.eq(course.id)).
            innerJoin(course.teacher).fetchJoin().
            where(learn.userId.id.eq(studentId)).fetch();

            return enrolledCourses;

        } catch (Exception e) {
            log.error("Error occurred:", e.getMessage());
            throw e;
        }
    }

    private List<CourseEntity> getCreatedCourses(Integer studentId) {
        try {
            List<CourseEntity> createdCourses = queryFactory.selectFrom(course).
            innerJoin(course.teacher).fetchJoin().
            where(course.teacher.id.eq(studentId)).fetch();

            return createdCourses;

        } catch (Exception e) {
            log.error("Error occurred:",e.getMessage());
            throw e;
        }
    }


    @Transactional
    public Long addCourseFile(MultipartFile attachment, Long courseId, Integer userId){
        var uploadDto = LocalFileUploadDTO.builder()
                .file(attachment)
                .entityType(EntityType.COURSE)
                .entityId(courseId)
                .userId(userId)
                .directory(String.format("course_%d/", courseId))
                .build();
        var file = fileService.uploadAndLink(uploadDto);
        CourseContentEntity courseContent = new CourseContentEntity();
        courseContent.setCourse(courseRepository.getReferenceById(courseId));
        courseContent.setFile(fileReferenceRepository.getReferenceById(file.getId()));
        courseContentRepository.save(courseContent);

        return file.getId();
    }

    @Transactional
    public void deleteCourseFile(Long fileId){
        fileService.deleteFile(fileId);
        courseContentRepository.deleteById(fileId);
    }

    @Transactional
    public Long addCourseUnitFile(MultipartFile attachment, Long courseUnitId, Integer userId){
        var uploadDto = LocalFileUploadDTO.builder()
                .file(attachment)
                .entityType(EntityType.COURSEUNIT)
                .entityId(courseUnitId)
                .userId(userId)
                .directory(String.format("course_unit_%d/", courseUnitId))
                .build();
        var file = fileService.uploadAndLink(uploadDto);

        CourseUnitContentEntity courseUnitContent = new CourseUnitContentEntity();
        courseUnitContent.setCourseUnit(courseUnitRepository.getReferenceById(courseUnitId));
        courseUnitContent.setFileReference(fileReferenceRepository.getReferenceById(file.getId()));
        courseUnitContentRepository.save(courseUnitContent);

        return file.getId();

    }

    @Transactional
    public void deleteCourseUnitFile(Long fileId){
        fileService.deleteFile(fileId);
        courseUnitContentRepository.deleteById(fileId);
        // fileReferenceRepository.deleteByEntityId(fileId);
    }

    public List<FileResponse> getCourseUnitFiles(Long courseUnitId){
        List<FileResponse> courseUnitFiles = fileService.getFileReferencesByEntity(EntityType.COURSEUNIT, courseUnitId);
        return courseUnitFiles;
    }


    @Transactional
    public UserEnrolledAndCreatedCoursesDTO getUserCourses(Integer studentId) {
        try {
            List<CourseEntity> enrolledCourses = getEnrolledCourses(studentId);
            System.out.println("enrolled courses received");
            List<CourseEntity> createdCourses = getCreatedCourses(studentId);
            System.out.println("created courses received");


            UserEnrolledAndCreatedCoursesDTO userCourses = new UserEnrolledAndCreatedCoursesDTO();
            userCourses.setCreatedCourses(createdCourses);
            userCourses.setEnrolledCourses(enrolledCourses);

            return userCourses;


        } catch (Exception e) {
            log.info("Error occurred:",e.getMessage());
            throw e;
        }
    }

    public List<Long> getCourseFiles(Long courseId) {
        List<Long> courseFileIds = queryFactory.select(courseContent.file.id)
        .from(courseContent)
        .where(courseContent.course.id.eq(courseId))
        .fetch();

        return courseFileIds;


    }

    private static final QCourseEntity course = QCourseEntity.courseEntity;
    private static final QCourseUnitEntity courseUnit = QCourseUnitEntity.courseUnitEntity;
    private static final QAssignmentEntity assignment = QAssignmentEntity.assignmentEntity;
    private static final QUserEntity user = QUserEntity.userEntity;
    private static final QUserCourseRelationEntity userCourseRelation = QUserCourseRelationEntity.userCourseRelationEntity;
    private static final QSubmissionEntity submission = QSubmissionEntity.submissionEntity;
    private static final QLearnEntity learn = QLearnEntity.learnEntity;
    private static final QFileReferenceEntity fileReference = QFileReferenceEntity.fileReferenceEntity;
    private static final QCourseContentEntity courseContent = QCourseContentEntity.courseContentEntity;
}
