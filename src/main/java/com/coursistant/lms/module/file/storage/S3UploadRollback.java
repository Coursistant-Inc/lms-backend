package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Remembers object keys put during one business call and enqueues independent
 * S3 abort rows if that call fails without a committed database transaction.
 * The bean holds no per-request state; each {@link #open} returns an isolated {@link Scope}.
 */
@Component
public class S3UploadRollback {

    private static final Logger log = LoggerFactory.getLogger(S3UploadRollback.class);

    private final MinioOutboxService minioOutboxService;

    public S3UploadRollback(MinioOutboxService minioOutboxService) {
        this.minioOutboxService = minioOutboxService;
    }

    public Scope open(Integer courseId, String uploadOperationId) {
        return new Scope(courseId, uploadOperationId);
    }

    public final class Scope {

        private final Integer courseId;
        private final String uploadOperationId;
        private final LinkedHashSet<UploadedKey> keys = new LinkedHashSet<>();
        private boolean syncRegistered;

        private Scope(Integer courseId, String uploadOperationId) {
            this.courseId = courseId;
            this.uploadOperationId = uploadOperationId;
        }

        public void remember(String logicalBucket, String objectKey) {
            if (logicalBucket == null || logicalBucket.isBlank() || objectKey == null || objectKey.isBlank()) {
                return;
            }
            keys.add(new UploadedKey(logicalBucket, objectKey));
            registerSyncIfNeeded();
        }

        /**
         * Compensates immediately unless a rollback {@code afterCompletion} callback
         * was successfully registered. An active transaction without synchronization
         * does not skip compensation.
         */
        public void abortIfNoTransaction() {
            if (syncRegistered) {
                return;
            }
            abortRemembered();
        }

        private void registerSyncIfNeeded() {
            if (syncRegistered) {
                return;
            }
            if (!TransactionSynchronizationManager.isActualTransactionActive()
                    || !TransactionSynchronizationManager.isSynchronizationActive()) {
                return;
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        abortRemembered();
                    }
                }
            });
            syncRegistered = true;
        }

        private void abortRemembered() {
            for (UploadedKey key : keys) {
                try {
                    minioOutboxService.enqueueAbortStagingIndependent(
                            key.bucket, key.objectKey, courseId, uploadOperationId);
                } catch (DuplicateKeyException e) {
                    log.debug("Abort outbox already pending for bucket={} key={}", key.bucket, key.objectKey);
                } catch (RuntimeException e) {
                    log.warn("Failed to enqueue S3 abort for bucket={} key={}: {}",
                            key.bucket, key.objectKey, e.getMessage());
                }
            }
        }
    }

    private record UploadedKey(String bucket, String objectKey) {
        private UploadedKey {
            Objects.requireNonNull(bucket);
            Objects.requireNonNull(objectKey);
        }
    }
}
