package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationPolicyTest {

    @Test
    void coversEveryNotificationType() {
        for (NotificationType type : NotificationType.values()) {
            NotificationPolicy.Mapping mapping = NotificationPolicy.forType(type);
            switch (type) {
                case ANNOUNCEMENT_POSTED, COURSE_EVENT_CREATED -> {
                    assertEquals(NotificationPolicy.Audience.ALL_ACTIVE_COURSE_MEMBERS, mapping.audience());
                    assertEquals(NotificationPolicy.EmailMode.DIGEST, mapping.emailMode());
                }
                case ASSIGNMENT_PUBLISHED, WEEK_PUBLISHED, ASSIGNMENT_SCHEDULE_CHANGED,
                     QUIZ_PUBLISHED, QUIZ_SCHEDULE_CHANGED, QUIZ_TIME_LIMIT_CHANGED -> {
                    assertEquals(NotificationPolicy.Audience.ACTIVE_STUDENTS, mapping.audience());
                    assertEquals(NotificationPolicy.EmailMode.DIGEST, mapping.emailMode());
                }
                case GROUP_MEMBER_ADDED, GROUP_MEMBER_REMOVED, GROUP_MEMBER_MOVED -> {
                    assertEquals(NotificationPolicy.Audience.PROVIDED_RECIPIENTS, mapping.audience());
                    assertEquals(NotificationPolicy.EmailMode.DIGEST, mapping.emailMode());
                }
                case ASSIGNMENT_SUBMISSION_RECEIVED, ASSIGNMENT_GRADE_RELEASED, ASSIGNMENT_GRADE_CORRECTED,
                     QUIZ_GRADE_RELEASED, QUIZ_GRADE_CORRECTED -> {
                    assertEquals(NotificationPolicy.Audience.PROVIDED_RECIPIENTS, mapping.audience());
                    assertEquals(NotificationPolicy.EmailMode.IMMEDIATE, mapping.emailMode());
                }
            }
        }
        assertEquals(16, NotificationType.values().length);
    }

    @Test
    void nullType_throws() {
        assertThrows(IllegalArgumentException.class, () -> NotificationPolicy.forType(null));
    }
}
