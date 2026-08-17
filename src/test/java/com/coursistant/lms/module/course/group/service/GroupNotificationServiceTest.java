package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupNotificationServiceTest {

    @Mock private NotificationPublisher notificationPublisher;
    @Mock private CourseGroupMapper courseGroupMapper;
    @Mock private UserMapper userMapper;
    @InjectMocks private GroupNotificationService service;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "notificationMessageFactory",
                new NotificationMessageFactory());
        NotificationTimeSupport time = org.mockito.Mockito.mock(NotificationTimeSupport.class);
        org.mockito.Mockito.lenient().when(time.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 17, 1, 0));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "notificationTimeSupport", time);
        org.mockito.Mockito.lenient().when(notificationPublisher.publishInTransaction(any())).thenReturn(1L);
    }

    @Test
    void notifyAdded_splitsTargetAndMembers_andExcludesStaffActorFromMembers() {
        Course course = course();
        when(courseGroupMapper.selectById(4)).thenReturn(group(4, "Alpha"));
        User student = new User();
        student.setId(50);
        student.setName("Pat");
        when(userMapper.selectById(50)).thenReturn(student);

        service.notifyAdded(course, 7, 4, 50, GroupMembershipAudit.ACTOR_USER, 9, 100,
                List.of(50, 51, 9));

        ArgumentCaptor<NotificationDispatchPayload> captor =
                ArgumentCaptor.forClass(NotificationDispatchPayload.class);
        verify(notificationPublisher, times(2)).publishInTransaction(captor.capture());
        List<NotificationDispatchPayload> payloads = captor.getAllValues();
        NotificationDispatchPayload target = payloads.get(0);
        NotificationDispatchPayload members = payloads.get(1);
        assertEquals(NotificationType.GROUP_MEMBER_ADDED, target.getNotificationType());
        assertEquals("group-membership-change:100:added:target", target.getEventKey());
        assertEquals(List.of(50), target.getRecipientIds());
        assertTrue(target.getMessage().contains("You joined"));
        assertEquals("group-membership-change:100:added:members", members.getEventKey());
        assertEquals(List.of(51), members.getRecipientIds());
        assertFalse(members.getRecipientIds().contains(50));
        assertFalse(members.getRecipientIds().contains(9));
    }

    @Test
    void notifyMoved_writesThreeVariants_withOldAndNewNamesOnTarget() {
        Course course = course();
        when(courseGroupMapper.selectById(1)).thenReturn(group(1, "Old"));
        when(courseGroupMapper.selectById(2)).thenReturn(group(2, "New"));
        User student = new User();
        student.setId(50);
        student.setName("Pat");
        when(userMapper.selectById(50)).thenReturn(student);

        service.notifyMoved(course, 7, 1, 2, 50, GroupMembershipAudit.ACTOR_USER, 50, 200,
                List.of(60), List.of(50, 70));

        ArgumentCaptor<NotificationDispatchPayload> captor =
                ArgumentCaptor.forClass(NotificationDispatchPayload.class);
        verify(notificationPublisher, times(3)).publishInTransaction(captor.capture());
        List<NotificationDispatchPayload> payloads = captor.getAllValues();
        assertEquals("group-membership-change:200:moved:target", payloads.get(0).getEventKey());
        assertTrue(payloads.get(0).getMessage().contains("Old"));
        assertTrue(payloads.get(0).getMessage().contains("New"));
        assertEquals(List.of(60), payloads.get(1).getRecipientIds());
        assertEquals(List.of(70), payloads.get(2).getRecipientIds());
    }

    @Test
    void emptyMemberVariant_isSkipped() {
        Course course = course();
        when(courseGroupMapper.selectById(4)).thenReturn(group(4, "Alpha"));
        service.notifyAdded(course, 7, 4, 50, GroupMembershipAudit.ACTOR_USER, 9, 100, List.of(50));
        verify(notificationPublisher, times(1)).publishInTransaction(any());
    }

    @Test
    void archivedCourse_skips() {
        Course course = course();
        course.setState("Archived");
        service.notifyRemoved(course, 7, 4, 50, GroupMembershipAudit.ACTOR_USER, 9, 100, List.of(51));
        verify(notificationPublisher, never()).publishInTransaction(any());
    }

    private Course course() {
        Course course = new Course();
        course.setId(3);
        course.setTenantId(1);
        course.setCourseCode("CS101");
        course.setTitle("Intro");
        course.setState("Active");
        return course;
    }

    private CourseGroup group(int id, String name) {
        CourseGroup group = new CourseGroup();
        group.setId(id);
        group.setName(name);
        return group;
    }
}
