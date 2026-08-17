package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPolicyTest {

    @Test
    void digestEvents_useCourseActiveStudents() {
        NotificationPolicy.Mapping mapping = NotificationPolicy.forType(NotificationType.ANNOUNCEMENT_POSTED);
        assertEquals(RecipientMode.COURSE_ACTIVE_STUDENTS, mapping.recipientMode());
        assertEquals(NotificationPolicy.EmailMode.DIGEST, mapping.emailMode());
    }

    @Test
    void gradeEvents_useExplicitImmediate() {
        NotificationPolicy.Mapping mapping = NotificationPolicy.forType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
        assertEquals(RecipientMode.EXPLICIT, mapping.recipientMode());
        assertEquals(NotificationPolicy.EmailMode.IMMEDIATE, mapping.emailMode());
    }
}
