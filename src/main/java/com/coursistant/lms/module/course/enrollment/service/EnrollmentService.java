package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.dto.MyCourseResponse;
import com.coursistant.lms.module.course.enrollment.dto.PromoteTaResponse;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.entity.EnrollmentAuditLog;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentAuditLogMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.util.EmailUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private static final String ROLE_INSTRUCTOR = "Instructor";
    private static final String ROLE_TA = "TA";
    private static final String ROLE_STUDENT = "Student";
    private static final String STATE_ARCHIVED = "Archived";

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private EnrollmentAuditLogMapper enrollmentAuditLogMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EmailUtil emailUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Enrolls the course's instructor. Called from {@code CourseService.create} within the
     * same transaction, so any failure here rolls back the course creation as well.
     */
    @Transactional
    public void createInstructorEnrollment(Integer courseId, Integer instructorUserId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(instructorUserId);
        enrollment.setCourseRole(ROLE_INSTRUCTOR);
        enrollment.setCanGrade(true);
        enrollment.setCanPostAnnouncements(true);
        enrollment.setCanManageGroups(true);
        enrollment.setCanManageCourseEvents(true);
        enrollment.setActive(true);
        enrollment.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
        enrollmentMapper.insert(enrollment);

        writeAudit(courseId, instructorUserId, ACTOR_TYPE_USER, instructorUserId, "INSTRUCTOR_ENROLLED", null);
    }

    @Transactional
    public MemberResponse adminEnrollStudent(Integer courseId, Integer userId, Integer adminId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        requireUser(userId);

        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (existing != null) {
            throw new ApiException(ErrorType.CONFLICT, "User is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(userId);
        enrollment.setCourseRole(ROLE_STUDENT);
        enrollment.setCanGrade(false);
        enrollment.setCanPostAnnouncements(false);
        enrollment.setCanManageGroups(false);
        enrollment.setCanManageCourseEvents(false);
        enrollment.setActive(true);
        enrollment.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
        enrollmentMapper.insert(enrollment);

        writeAudit(courseId, userId, ACTOR_TYPE_ADMIN, adminId, "STUDENT_ENROLLED", null);
        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    public List<MemberResponse> listMembers(Integer courseId) {
        requireCourse(courseId);
        return enrollmentMapper.selectByCourseId(courseId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromoteTaResponse promoteToTa(Integer courseId, Integer userId, UpdateTaPermissionsRequest request, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = requireMember(courseId, userId);
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_ACTIVE);
        }
        if (!ROLE_STUDENT.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.INVALID_ROLE_TRANSITION, "Only a Student can be promoted to TA");
        }

        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setCourseRole(ROLE_TA);
        patch.setCanGrade(request != null && Boolean.TRUE.equals(request.getCanGrade()));
        patch.setCanPostAnnouncements(request != null && Boolean.TRUE.equals(request.getCanPostAnnouncements()));
        patch.setCanManageGroups(request != null && Boolean.TRUE.equals(request.getCanManageGroups()));
        patch.setCanManageCourseEvents(request != null && Boolean.TRUE.equals(request.getCanManageCourseEvents()));
        enrollmentMapper.updateById(patch);

        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "PROMOTED_TO_TA", toJson(request));
        sendTaChangeEmailBestEffort(userId, course, "promoted to Teaching Assistant");

        PromoteTaResponse response = new PromoteTaResponse();
        response.setMember(toMemberResponse(requireEnrollmentById(enrollment.getId())));
        response.setWarnings(new ArrayList<>());
        return response;
    }

    @Transactional
    public MemberResponse revokeTa(Integer courseId, Integer userId, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = requireMember(courseId, userId);
        if (!ROLE_TA.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.INVALID_ROLE_TRANSITION, "User is not a TA in this course");
        }

        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setCourseRole(ROLE_STUDENT);
        patch.setCanGrade(false);
        patch.setCanPostAnnouncements(false);
        patch.setCanManageGroups(false);
        patch.setCanManageCourseEvents(false);
        enrollmentMapper.updateById(patch);

        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "TA_REVOKED", null);
        sendTaChangeEmailBestEffort(userId, course, "removed as Teaching Assistant");

        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    @Transactional
    public MemberResponse updateTaPermissions(Integer courseId, Integer userId, UpdateTaPermissionsRequest request, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = requireMember(courseId, userId);
        if (!ROLE_TA.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.INVALID_ROLE_TRANSITION, "User is not a TA in this course");
        }
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }

        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setCanGrade(request.getCanGrade());
        patch.setCanPostAnnouncements(request.getCanPostAnnouncements());
        patch.setCanManageGroups(request.getCanManageGroups());
        patch.setCanManageCourseEvents(request.getCanManageCourseEvents());
        enrollmentMapper.updateById(patch);

        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "TA_PERMISSIONS_UPDATED", toJson(request));
        sendTaChangeEmailBestEffort(userId, course, "had their Teaching Assistant permissions updated");

        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    public List<Integer> listActiveCourseIdsForUser(Integer userId) {
        return enrollmentMapper.selectActiveByUserId(userId).stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());
    }

    public List<MyCourseResponse> listMyCourses(Integer userId) {
        List<MyCourseResponse> result = new ArrayList<>();
        for (Enrollment enrollment : enrollmentMapper.selectActiveByUserId(userId)) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course == null) {
                continue;
            }
            result.add(toMyCourseResponse(enrollment, course));
        }
        return result;
    }

    public int countByCourseId(Integer courseId) {
        return enrollmentMapper.countByCourseId(courseId);
    }

    private void writeAudit(Integer courseId, Integer targetUserId, String actorType, Integer actorId, String action, String detailJson) {
        EnrollmentAuditLog auditLog = new EnrollmentAuditLog();
        auditLog.setCourseId(courseId);
        auditLog.setTargetUserId(targetUserId);
        auditLog.setActorType(actorType);
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setDetailJson(detailJson);
        enrollmentAuditLogMapper.insert(auditLog);
    }

    private void sendTaChangeEmailBestEffort(Integer userId, Course course, String actionDescription) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                String subject = "Course role update: " + course.getTitle();
                String content = "You have been " + actionDescription + " for course " + course.getTitle() + ".";
                emailUtil.sendEmail(user.getEmail(), subject, content);
            }
        } catch (Exception e) {
            log.warn("Failed to send TA change email to user {} for course {}: {}", userId, course.getId(), e.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize audit detail: {}", e.getMessage());
            return null;
        }
    }

    private Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void requireNotArchived(Course course) {
        if (STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    private void requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
    }

    private Enrollment requireMember(Integer courseId, Integer userId) {
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        return enrollment;
    }

    private Enrollment requireEnrollmentById(Integer id) {
        Enrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        return enrollment;
    }

    private MemberResponse toMemberResponse(Enrollment enrollment) {
        MemberResponse response = new MemberResponse();
        response.setId(enrollment.getId());
        response.setCourseId(enrollment.getCourseId());
        response.setUserId(enrollment.getUserId());
        response.setCourseRole(enrollment.getCourseRole());
        response.setCanGrade(enrollment.getCanGrade());
        response.setCanPostAnnouncements(enrollment.getCanPostAnnouncements());
        response.setCanManageGroups(enrollment.getCanManageGroups());
        response.setCanManageCourseEvents(enrollment.getCanManageCourseEvents());
        response.setActive(enrollment.getActive());
        response.setEnrolledAt(enrollment.getEnrolledAt());
        response.setCreatedAt(enrollment.getCreatedAt());
        response.setUpdatedAt(enrollment.getUpdatedAt());

        User user = userMapper.selectById(enrollment.getUserId());
        if (user != null) {
            response.setUserName(user.getName());
            response.setUserEmail(user.getEmail());
        }
        return response;
    }

    private MyCourseResponse toMyCourseResponse(Enrollment enrollment, Course course) {
        MyCourseResponse response = new MyCourseResponse();
        response.setId(course.getId());
        response.setTenantId(course.getTenantId());
        response.setCourseCode(course.getCourseCode());
        response.setTitle(course.getTitle());
        response.setTermStartDate(course.getTermStartDate());
        response.setTermEndDate(course.getTermEndDate());
        response.setDescription(course.getDescription());
        response.setLocation(course.getLocation());
        response.setInstructorId(course.getInstructorId());
        response.setState(course.getState());
        response.setArchivedAt(course.getArchivedAt());
        response.setRole(enrollment.getCourseRole());
        return response;
    }
}
