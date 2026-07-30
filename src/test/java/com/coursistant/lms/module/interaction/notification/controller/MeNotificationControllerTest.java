package com.coursistant.lms.module.interaction.notification.controller;

import com.coursistant.lms.module.interaction.notification.dto.NotificationPageResponse;
import com.coursistant.lms.module.interaction.notification.dto.NotificationResponse;
import com.coursistant.lms.module.interaction.notification.dto.UnreadCountResponse;
import com.coursistant.lms.module.interaction.notification.service.NotificationService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiExceptionHandler;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeNotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MeNotificationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        User user = new User();
        user.setId(10);
        user.setTenantId(1);
        when(userMapper.selectById(10)).thenReturn(user);
    }

    @Test
    void list_returnsPagedItems() throws Exception {
        NotificationPageResponse page = new NotificationPageResponse();
        NotificationResponse item = new NotificationResponse();
        item.setNotificationId(5);
        item.setMessage("Hello");
        page.setItems(List.of(item));
        page.setPage(1);
        page.setSize(20);
        page.setTotal(1);
        when(notificationService.list(eq(10), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/v2/me/notifications").requestAttr("userId", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].notificationId").value(5));
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        when(notificationService.unreadCount(10)).thenReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/v2/me/notifications/unread-count").requestAttr("userId", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void markRead_crossUser_returnsNotFound() throws Exception {
        doThrow(new ApiException(ErrorType.NOT_FOUND)).when(notificationService).markRead(10, 99);

        mockMvc.perform(patch("/v2/me/notifications/99/read").requestAttr("userId", 10))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void markAllRead_returnsZero() throws Exception {
        when(notificationService.markAllRead(10)).thenReturn(new UnreadCountResponse(0));

        mockMvc.perform(patch("/v2/me/notifications/read-all").requestAttr("userId", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
        verify(notificationService).markAllRead(10);
    }
}
