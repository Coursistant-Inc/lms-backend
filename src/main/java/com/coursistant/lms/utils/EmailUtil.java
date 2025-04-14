package com.coursistant.lms.utils;

import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class EmailUtil {

    @Resource
    private JavaMailSender mailSender;

    private static final String FROM_EMAIL = "do.not.reply@coursistant.com";
    // 替换为你的邮箱 // Replace with your email

    /**
     * 发送邮件 // Send email
     */
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new CustomException(ResultCodeEnum.EMAIL_NOT_SEND_ERROR);
        }
    }
}
