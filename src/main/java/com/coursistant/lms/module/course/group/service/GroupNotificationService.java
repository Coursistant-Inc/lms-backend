package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * V1 log-only group notifications. Archived courses never generate notifications.
 */
@Service
public class GroupNotificationService {

    private static final Logger log = LoggerFactory.getLogger(GroupNotificationService.class);
    private static final String STATE_ARCHIVED = "Archived";

    public void notifyMembershipChanged(Course course, String action, Integer targetUserId, Integer groupId) {
        if (course == null || STATE_ARCHIVED.equals(course.getState()) || course.getArchivedAt() != null) {
            log.debug("Skip group notification for archived course: action={}, userId={}, groupId={}",
                    action, targetUserId, groupId);
            return;
        }
        log.info("Group notification: courseId={}, action={}, userId={}, groupId={}",
                course.getId(), action, targetUserId, groupId);
    }
}
