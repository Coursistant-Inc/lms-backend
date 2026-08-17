package com.coursistant.lms.module.interaction.notification.it.support;

import com.coursistant.lms.module.assignment.service.AssignmentNotificationService;
import com.coursistant.lms.module.course.group.service.GroupNotificationService;
import com.coursistant.lms.module.quiz.service.QuizNotificationService;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        AssignmentNotificationService.class,
        QuizNotificationService.class,
        GroupNotificationService.class,
        TenantTimezoneService.class
})
@MapperScan({
        "com.coursistant.lms.module.assignment.repository",
        "com.coursistant.lms.module.quiz.repository",
        "com.coursistant.lms.module.course.group.repository",
        "com.coursistant.lms.module.course.content.week.repository"
})
public class NotificationPhase2ExtraConfig {
}
