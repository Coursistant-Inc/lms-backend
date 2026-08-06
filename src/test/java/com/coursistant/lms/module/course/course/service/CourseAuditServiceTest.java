package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.CourseAuditLog;
import com.coursistant.lms.module.course.course.repository.CourseAuditLogMapper;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAuditServiceTest {

    @Mock
    private CourseAuditLogMapper courseAuditLogMapper;

    @InjectMocks
    private CourseAuditService courseAuditService;

    @Test
    void write_persistsActorAndJsonWithoutSecrets() {
        when(courseAuditLogMapper.insert(any())).thenAnswer(inv -> {
            CourseAuditLog row = inv.getArgument(0);
            row.setId(42L);
            return 1;
        });
        ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 7, RoleEnum.USER.name(), 1, "INSTRUCTOR", "ACTIVE");

        Long id = courseAuditService.write(actor, 10, 1, "COURSE_VIEWED", "COURSE", 10,
                Map.of("state", "Active"), Map.of("state", "Active"), "req-1");

        assertEquals(42L, id);
        ArgumentCaptor<CourseAuditLog> cap = ArgumentCaptor.forClass(CourseAuditLog.class);
        verify(courseAuditLogMapper).insert(cap.capture());
        CourseAuditLog row = cap.getValue();
        assertEquals(10, row.getCourseId());
        assertEquals("USER", row.getActorType());
        assertEquals(7, row.getActorId());
        assertEquals("COURSE_VIEWED", row.getAction());
        assertNotNull(row.getBeforeJson());
        assertFalse(row.getBeforeJson().toLowerCase().contains("password"));
        assertFalse(row.getBeforeJson().toLowerCase().contains("token"));
    }
}
