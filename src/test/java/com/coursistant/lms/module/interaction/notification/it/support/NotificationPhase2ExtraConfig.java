package com.coursistant.lms.module.interaction.notification.it.support;

import com.coursistant.lms.module.assignment.service.AssignmentAccessService;
import com.coursistant.lms.module.assignment.service.AssignmentAuditService;
import com.coursistant.lms.module.assignment.service.AssignmentFilePolicy;
import com.coursistant.lms.module.assignment.service.AssignmentNotificationService;
import com.coursistant.lms.module.assignment.service.AssignmentResponseAssembler;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.module.assignment.service.AssignmentTimeSupport;
import com.coursistant.lms.module.assignment.service.SubmissionStatusCalculator;
import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.material.service.MaterialResponseAssembler;
import com.coursistant.lms.module.course.content.week.service.CourseWeekService;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.group.service.GroupAccessService;
import com.coursistant.lms.module.course.group.service.GroupAuditService;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import com.coursistant.lms.module.course.group.service.GroupNotificationService;
import com.coursistant.lms.module.course.group.service.GroupResponseAssembler;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.quiz.service.QuizAccessService;
import com.coursistant.lms.module.quiz.service.QuizAuditService;
import com.coursistant.lms.module.quiz.service.QuizAuthoringService;
import com.coursistant.lms.module.quiz.service.QuizNotificationService;
import com.coursistant.lms.module.quiz.service.QuizQuestionService;
import com.coursistant.lms.module.quiz.service.QuizTimeSupport;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.shared.security.AuthzService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        AssignmentNotificationService.class,
        AssignmentService.class,
        AssignmentAccessService.class,
        AssignmentAuditService.class,
        AssignmentTimeSupport.class,
        AssignmentFilePolicy.class,
        AssignmentResponseAssembler.class,
        SubmissionStatusCalculator.class,
        AuthzService.class,
        QuizNotificationService.class,
        QuizAuthoringService.class,
        QuizAccessService.class,
        QuizTimeSupport.class,
        QuizAuditService.class,
        QuizQuestionService.class,
        CourseWeekService.class,
        CourseContentAccessService.class,
        CourseAuthorizationService.class,
        CourseAuditService.class,
        MaterialResponseAssembler.class,
        CourseContentFilePolicy.class,
        S3ObjectKeyResolver.class,
        GroupNotificationService.class,
        GroupMembershipService.class,
        GroupAccessService.class,
        GroupResponseAssembler.class,
        GroupAuditService.class,
        CoursePermissionService.class,
        TenantTimezoneService.class
})
@MapperScan({
        "com.coursistant.lms.module.assignment.repository",
        "com.coursistant.lms.module.quiz.repository",
        "com.coursistant.lms.module.course.group.repository",
        "com.coursistant.lms.module.course.content.week.repository",
        "com.coursistant.lms.module.course.content.material.repository"
})
public class NotificationPhase2ExtraConfig {
}
