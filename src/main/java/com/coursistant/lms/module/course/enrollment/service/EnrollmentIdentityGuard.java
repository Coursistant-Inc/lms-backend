package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blocks illegal Role/Level/Status/Tenant changes that would leave Active Enrollment inconsistent.
 * Lock order: User FOR UPDATE first (caller or here), then enrollments.
 * <p>
 * Compatibility matrix: any Active Enrollment requires role=USER;
 * Active Instructor requires level=INSTRUCTOR; Active Student/TA requires level=STUDENT.
 */
@Service
public class EnrollmentIdentityGuard {

    @Resource
    private UserMapper userMapper;
    @Resource
    private EnrollmentMapper enrollmentMapper;
    @Resource
    private EnrollmentMembershipService enrollmentMembershipService;

    /**
     * Call before applying role/level change. Locks user row.
     */
    @Transactional
    public User assertCanChangeRoleOrLevel(Integer userId, String newRole, String newLevel) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }

        int activeInstructor = enrollmentMapper.countActiveInstructorEnrollmentsByUserId(userId);
        int activeTa = enrollmentMapper.countActiveTaEnrollmentsByUserId(userId);
        int activeStudent = enrollmentMapper.countActiveStudentEnrollmentsByUserId(userId);
        boolean hasActiveEnrollment = activeInstructor > 0 || activeTa > 0 || activeStudent > 0;

        if (hasActiveEnrollment) {
            if (newRole != null && !RoleEnum.USER.name().equals(newRole)) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User still has Active enrollments; role must remain USER");
            }
        }
        if (activeInstructor > 0) {
            if (newLevel != null && !LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(newLevel)) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User is still an Active Primary Instructor; reassign before changing role/level");
            }
        }
        if (activeTa > 0 || activeStudent > 0) {
            if (newLevel != null && !LevelEnum.STUDENT.level.equalsIgnoreCase(newLevel)) {
                String message = activeTa > 0
                        ? "User still has Active TA enrollments; remove TA before changing role/level"
                        : "User still has Active Student enrollments; withdraw before changing role/level";
                throw new ApiException(ErrorType.CONFLICT, message);
            }
        }
        return user;
    }

    @Transactional
    public User assertCanChangeTenant(Integer userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (enrollmentMapper.countByUserId(userId) > 0) {
            throw new ApiException(ErrorType.USER_TENANT_CHANGE_BLOCKED);
        }
        return user;
    }

    /**
     * Disable account: reject if Active Primary Instructor; otherwise soft-withdraw TA/Student.
     */
    @Transactional
    public User disableAccountWithEnrollmentWithdraw(ActorContext actor, Integer userId, String requestId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (enrollmentMapper.countActiveInstructorEnrollmentsByUserId(userId) > 0) {
            throw new ApiException(ErrorType.CONFLICT,
                    "User is still an Active Primary Instructor; reassign before disable");
        }
        enrollmentMembershipService.withdrawAllActiveNonInstructorForDisable(actor, userId, requestId);
        return user;
    }
}
