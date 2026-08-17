package com.coursistant.lms.module.interaction.notification.it.support;

import com.coursistant.lms.module.interaction.notification.controller.AdminNotificationController;
import com.coursistant.lms.module.interaction.notification.controller.MeNotificationController;
import com.coursistant.lms.module.interaction.notification.email.FakeNotificationEmailSender;
import com.coursistant.lms.module.interaction.notification.email.LoggingNotificationEmailSender;
import com.coursistant.lms.module.interaction.notification.email.SmtpNotificationEmailSender;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootConfiguration
@EnableTransactionManagement
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        MailSenderAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.coursistant.lms.module.interaction.notification",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        AdminNotificationController.class,
                        MeNotificationController.class,
                        LoggingNotificationEmailSender.class,
                        SmtpNotificationEmailSender.class
                })
        }
)
@MapperScan({
        "com.coursistant.lms.module.interaction.notification.repository",
        "com.coursistant.lms.module.user.account.repository",
        "com.coursistant.lms.module.course.course.repository",
        "com.coursistant.lms.module.course.enrollment.repository",
        "com.coursistant.lms.module.tenant.repository"
})
public class NotificationPhase1SpringITConfig {

    @Bean
    @Primary
    public FakeNotificationEmailSender fakeNotificationEmailSender() {
        return new FakeNotificationEmailSender();
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
