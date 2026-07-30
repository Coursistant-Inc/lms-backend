package com.coursistant.lms.module.interaction.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs actions after the current transaction commits (or immediately if none).
 * Failures are logged and never fail the business operation.
 */
@Component
public class NotificationSupport {

    private static final Logger log = LoggerFactory.getLogger(NotificationSupport.class);

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

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Notification afterCommit action failed (ignored): {}", e.getMessage());
        }
    }
}
