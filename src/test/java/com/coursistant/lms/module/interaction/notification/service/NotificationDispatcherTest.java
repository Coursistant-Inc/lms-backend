package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationWriteService notificationWriteService;

    @InjectMocks
    private NotificationDispatcher notificationDispatcher;

    @Test
    void dispatchAsync_chunks501RecipientsIntoTwoInserts() {
        when(notificationWriteService.insertChunk(anyList())).thenReturn(500);

        List<Integer> recipients = new ArrayList<>();
        for (int i = 1; i <= 501; i++) {
            recipients.add(i);
        }
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(1);
        payload.setCourseId(2);
        payload.setNotificationType(NotificationType.ASSIGNMENT_PUBLISHED);
        payload.setMessage("New assignment published: Homework");
        payload.setSubjectType(SubjectType.ASSIGNMENT);
        payload.setSubjectId(9);
        payload.setEventKey("published");
        payload.setDeepLink("/courses/2/assignments/9");
        payload.setRecipientIds(recipients);

        notificationDispatcher.dispatchAsync(payload);

        verify(notificationWriteService, times(2)).insertChunk(anyList());
    }
}
