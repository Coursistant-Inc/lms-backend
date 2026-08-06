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
        boolean toTenantAdmin = RoleEnum.TENANT_ADMIN.name().equals(newRole);
        boolean toStudentLevel = LevelEnum.STUDENT.level.equalsIgnoreCase(newLevel);
        boolean toInstructorLevel = LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(newLevel);

        if (enrollmentMapper.countActiveInstructorEnrollmentsByUserId(userId) > 0) {
            if (toTenantAdmin || toStudentLevel) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User is still an Active Primary Instructor; reassign before changing role/level");
            }
        }
        if (enrollmentMapper.countActiveTaEnrollmentsByUserId(userId) > 0) {
            if (toTenantAdmin || toStudentLevel) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User still has Active TA enrollments; remove TA before changing role/level");
            }
        }
        if (enrollmentMapper.countActiveStudentEnrollmentsByUserId(userId) > 0) {
            if (toTenantAdmin || toInstructorLevel) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User still has Active Student enrollments; withdraw before changing role/level");
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
