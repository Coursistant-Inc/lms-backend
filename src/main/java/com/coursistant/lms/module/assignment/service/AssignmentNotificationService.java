package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Best-effort assignment notifications. Tier-1 has no delivery channel wired up yet, so the
 * methods only log; what matters is the contract: notifications run <em>after</em> the
 * transaction commits and can never fail the business operation.
 */
@Service
public class AssignmentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentNotificationService.class);

    /**
     * Runs {@code action} once the current transaction commits, swallowing any failure.
     * Executes immediately when there is no active transaction.
     */
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

    public void notifyAssignmentPublished(Assignment assignment, List<Integer> studentUserIds) {
        runSafely(() -> log.info("Notify assignment published: courseId={}, assignmentId={}, recipients={}",
                assignment.getCourseId(), assignment.getId(), size(studentUserIds)));
    }

    public void notifyDueDateChanged(Assignment assignment, LocalDateTime previousDueAt, List<Integer> studentUserIds) {
        runSafely(() -> log.info("Notify due date changed: courseId={}, assignmentId={}, from={}, to={}, recipients={}",
                assignment.getCourseId(), assignment.getId(), previousDueAt, assignment.getDueAt(), size(studentUserIds)));
    }

    public void notifySubmissionReceived(Assignment assignment, Integer studentUserId, Integer versionNo,
                                         LocalDateTime submittedAt) {
        runSafely(() -> log.info("Notify submission receipt: courseId={}, assignmentId={}, userId={}, versionNo={}, submittedAt={}",
                assignment.getCourseId(), assignment.getId(), studentUserId, versionNo, submittedAt));
    }

    public void notifyGroupSubmissionReplaced(Assignment assignment, Integer groupId, Integer submitterUserId,
                                              Integer versionNo) {
        runSafely(() -> log.info(
                "Notify GROUP_SUBMISSION_REPLACED: courseId={}, assignmentId={}, groupId={}, submitterUserId={}, versionNo={}",
                assignment.getCourseId(), assignment.getId(), groupId, submitterUserId, versionNo));
    }

    public void notifyGradesReleased(Assignment assignment, List<Integer> studentUserIds) {
        runSafely(() -> log.info("Notify grades released: courseId={}, assignmentId={}, recipients={}",
                assignment.getCourseId(), assignment.getId(), size(studentUserIds)));
    }

    /**
     * Post-release score/feedback correction. Body must not include the numeric score.
     */
    public void notifyGradeCorrectedAfterRelease(Assignment assignment, Integer studentUserId) {
        runSafely(() -> log.info(
                "Notify grade corrected after release: courseId={}, assignmentId={}, userId={}",
                assignment.getCourseId(), assignment.getId(), studentUserId));
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Assignment notification failed (ignored): {}", e.getMessage());
        }
    }

    private int size(List<Integer> values) {
        return values == null ? 0 : values.size();
    }
}
