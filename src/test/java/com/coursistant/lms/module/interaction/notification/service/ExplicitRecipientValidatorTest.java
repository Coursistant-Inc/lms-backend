package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExplicitRecipientValidatorTest {

    @Mock private UserMapper userMapper;
    @InjectMocks private ExplicitRecipientValidator validator;

    @Test
    void keepsDroppedStudent_whenAccountActiveAndTenantMatches() {
        User user = new User();
        user.setId(50);
        user.setTenantId(3);
        user.setStatus("ACTIVE");
        when(userMapper.selectUsersByIds(List.of(50))).thenReturn(List.of(user));

        assertEquals(List.of(50), validator.validate(3, List.of(50)));
    }

    @Test
    void dropsDisabledAndCrossTenant() {
        User disabled = new User();
        disabled.setId(1);
        disabled.setTenantId(3);
        disabled.setStatus("DISABLED");
        User otherTenant = new User();
        otherTenant.setId(2);
        otherTenant.setTenantId(9);
        otherTenant.setStatus("ACTIVE");
        when(userMapper.selectUsersByIds(anyList())).thenReturn(List.of(disabled, otherTenant));

        assertEquals(List.of(), validator.validate(3, List.of(1, 2)));
    }

    @Test
    void submissionReceipt_doesNotExcludeActor() {
        assertFalse(validator.shouldExcludeActor(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED, 50, 50));
        assertTrue(validator.shouldExcludeActor(NotificationType.ASSIGNMENT_GRADE_RELEASED, 50, 50));
    }
}
