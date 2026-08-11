package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentIdentityGuardTest {

    @Mock private UserMapper userMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private EnrollmentMembershipService enrollmentMembershipService;

    @InjectMocks
    private EnrollmentIdentityGuard guard;

    @Test
    void primaryInstructor_blocksChangeToStudentOrTenantAdmin() {
        User u = user(1);
        when(userMapper.selectByIdForUpdate(1)).thenReturn(u);
        when(enrollmentMapper.countActiveInstructorEnrollmentsByUserId(1)).thenReturn(1);
        when(enrollmentMapper.countActiveTaEnrollmentsByUserId(1)).thenReturn(0);
        when(enrollmentMapper.countActiveStudentEnrollmentsByUserId(1)).thenReturn(0);

        ApiException toStudent = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(1, RoleEnum.USER.name(), "STUDENT"));
        assertEquals(ErrorType.CONFLICT, toStudent.getErrorType());

        ApiException toAdmin = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(1, RoleEnum.TENANT_ADMIN.name(), "NOT_APPLICABLE"));
        assertEquals(ErrorType.CONFLICT, toAdmin.getErrorType());
    }

    @Test
    void activeTa_allowsKeepStudent_blocksInstructorAndSystemAdmin() {
        User u = user(2);
        u.setLevel("STUDENT");
        when(userMapper.selectByIdForUpdate(2)).thenReturn(u);
        when(enrollmentMapper.countActiveInstructorEnrollmentsByUserId(2)).thenReturn(0);
        when(enrollmentMapper.countActiveTaEnrollmentsByUserId(2)).thenReturn(1);
        when(enrollmentMapper.countActiveStudentEnrollmentsByUserId(2)).thenReturn(0);

        assertDoesNotThrow(() -> guard.assertCanChangeRoleOrLevel(2, RoleEnum.USER.name(), "STUDENT"));

        ApiException toInstructor = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(2, RoleEnum.USER.name(), "INSTRUCTOR"));
        assertEquals(ErrorType.CONFLICT, toInstructor.getErrorType());

        ApiException toSys = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(2, RoleEnum.SYSTEM_ADMIN.name(), "NOT_APPLICABLE"));
        assertEquals(ErrorType.CONFLICT, toSys.getErrorType());

        ApiException toTenantAdmin = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(2, RoleEnum.TENANT_ADMIN.name(), "NOT_APPLICABLE"));
        assertEquals(ErrorType.CONFLICT, toTenantAdmin.getErrorType());

        ApiException toNa = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(2, "NOT_APPLICABLE", "NOT_APPLICABLE"));
        assertEquals(ErrorType.CONFLICT, toNa.getErrorType());
    }

    @Test
    void activeStudent_blocksTenantAdmin() {
        User u = user(4);
        u.setLevel("STUDENT");
        when(userMapper.selectByIdForUpdate(4)).thenReturn(u);
        when(enrollmentMapper.countActiveInstructorEnrollmentsByUserId(4)).thenReturn(0);
        when(enrollmentMapper.countActiveTaEnrollmentsByUserId(4)).thenReturn(0);
        when(enrollmentMapper.countActiveStudentEnrollmentsByUserId(4)).thenReturn(1);

        ApiException ex = assertThrows(ApiException.class,
                () -> guard.assertCanChangeRoleOrLevel(4, RoleEnum.TENANT_ADMIN.name(), "NOT_APPLICABLE"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());
    }

    @Test
    void disable_rejectsPrimaryInstructor_andWithdrawsOthers() {
        User u = user(3);
        when(userMapper.selectByIdForUpdate(3)).thenReturn(u);
        when(enrollmentMapper.countActiveInstructorEnrollmentsByUserId(3)).thenReturn(1);
        ActorContext actor = new ActorContext(ActorContext.ACTOR_ADMIN, 1, RoleEnum.SYSTEM_ADMIN.name(),
                null, null, "ACTIVE");
        ApiException ex = assertThrows(ApiException.class,
                () -> guard.disableAccountWithEnrollmentWithdraw(actor, 3, "k"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());

        when(enrollmentMapper.countActiveInstructorEnrollmentsByUserId(3)).thenReturn(0);
        guard.disableAccountWithEnrollmentWithdraw(actor, 3, "k");
        verify(enrollmentMembershipService).withdrawAllActiveNonInstructorForDisable(actor, 3, "k");
    }

    private User user(int id) {
        User u = new User();
        u.setId(id);
        u.setRole(RoleEnum.USER.name());
        u.setLevel("INSTRUCTOR");
        u.setStatus("ACTIVE");
        u.setTenantId(1);
        return u;
    }
}
