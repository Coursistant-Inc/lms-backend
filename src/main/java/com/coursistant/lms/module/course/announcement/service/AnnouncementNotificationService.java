package com.coursistant.lms.module.course.announcement.service;

import com.coursistant.lms.module.course.announcement.dto.NotificationResponse;
import com.coursistant.lms.module.course.announcement.entity.CourseAnnouncement;
import com.coursistant.lms.module.course.announcement.entity.UserNotification;
import com.coursistant.lms.module.course.announcement.repository.UserNotificationMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Best-effort durable notifications for announcements. Failures never fail the post.
 */
@Service
public class AnnouncementNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementNotificationService.class);
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Resource
    private UserNotificationMapper userNotificationMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    public void afterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action);
                }
            });
        } else {
            runSafely(action);
        }
    }

    /**
     * Inserts one ANNOUNCEMENT_POSTED notification per active member except the author.
     * Idempotent via UNIQUE (recipient, event_type, ref_id) + INSERT IGNORE.
     */
    public void notifyAnnouncementPosted(CourseAnnouncement announcement) {
        runSafely(() -> doNotifyAnnouncementPosted(announcement));
    }

    void doNotifyAnnouncementPosted(CourseAnnouncement announcement) {
        if (announcement == null || announcement.getId() == null || announcement.getCourseId() == null) {
            return;
        }
        List<Enrollment> members = enrollmentMapper.selectActiveByCourseId(announcement.getCourseId());
        if (members == null || members.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String deepLink = "/v2/courses/" + announcement.getCourseId() + "/announcements/" + announcement.getId();
        String title = announcement.getTitle() != null ? announcement.getTitle() : "New announcement";
        for (Enrollment member : members) {
            if (member.getUserId() == null) {
                continue;
            }
            if (member.getUserId().equals(announcement.getAuthorUserId())) {
                continue;
            }
            UserNotification n = new UserNotification();
            n.setRecipientUserId(member.getUserId());
            n.setCourseId(announcement.getCourseId());
            n.setEventType(UserNotification.EVENT_ANNOUNCEMENT_POSTED);
            n.setRefId(announcement.getId());
            n.setTitle(title);
            n.setDeepLink(deepLink);
            n.setCreatedAt(now);
            userNotificationMapper.insertIgnore(n);
        }
    }

    public List<NotificationResponse> listForUser(Integer userId, Integer limit) {
        int lim = normalizeLimit(limit, DEFAULT_LIMIT, MAX_LIMIT);
        return userNotificationMapper.selectByRecipient(userId, lim);
    }

    public void markRead(Integer userId, Integer notificationId) {
        if (notificationId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Notification id is required");
        }
        UserNotification existing = userNotificationMapper.selectById(notificationId);
        if (existing == null || !userId.equals(existing.getRecipientUserId())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Notification not found");
        }
        if (existing.getReadAt() != null) {
            return;
        }
        userNotificationMapper.markRead(notificationId, userId, LocalDateTime.now());
    }

    private int normalizeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Announcement notification failed (ignored): {}", e.getMessage());
        }
    }
}
