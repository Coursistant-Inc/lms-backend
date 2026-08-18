package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3UploadRollbackTest {

    @Mock
    private MinioOutboxService minioOutboxService;

    @AfterEach
    void clearTx() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void orphanV4_scopesDoNotShareKeys() {
        S3UploadRollback rollback = new S3UploadRollback(minioOutboxService);
        S3UploadRollback.Scope first = rollback.open(1, null);
        S3UploadRollback.Scope second = rollback.open(2, "op-2");
        first.remember("lms-uploads", "a.pdf");
        second.remember("lms-uploads", "b.pdf");

        first.abortIfNoTransaction();
        verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "a.pdf", 1, null);
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(
                eq("lms-uploads"), eq("b.pdf"), anyInt(), any());

        second.abortIfNoTransaction();
        verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "b.pdf", 2, "op-2");
    }

    @Test
    void orphanV5_rollbackCallbackEnqueues_commitDoesNot() {
        S3UploadRollback rollback = new S3UploadRollback(minioOutboxService);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        S3UploadRollback.Scope rolled = rollback.open(8, null);
        rolled.remember("lms-uploads", "fail.pdf");
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "fail.pdf", 8, null);

        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
        S3UploadRollback.Scope committed = rollback.open(8, null);
        committed.remember("lms-uploads", "ok.pdf");
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(
                eq("lms-uploads"), eq("ok.pdf"), anyInt(), any());
    }

    @Test
    void orphanV5_unknownStatusDoesNotDelete() {
        S3UploadRollback rollback = new S3UploadRollback(minioOutboxService);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        rollback.open(1, null).remember("lms-uploads", "maybe.pdf");
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
        }
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(any(), any(), any(), any());
    }

    @Test
    void orphanV6_duplicateIgnoredAndLaterKeysStillAttempted() {
        S3UploadRollback rollback = new S3UploadRollback(minioOutboxService);
        doThrow(new DuplicateKeyException("dup"))
                .doThrow(new RuntimeException("db down"))
                .doNothing()
                .when(minioOutboxService)
                .enqueueAbortStagingIndependent(eq("lms-uploads"), anyString(), eq(1), isNull());

        S3UploadRollback.Scope scope = rollback.open(1, null);
        scope.remember("lms-uploads", "one.pdf");
        scope.remember("lms-uploads", "two.pdf");
        scope.remember("lms-uploads", "three.pdf");
        scope.abortIfNoTransaction();

        var order = inOrder(minioOutboxService);
        order.verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "one.pdf", 1, null);
        order.verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "two.pdf", 1, null);
        order.verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "three.pdf", 1, null);
        verify(minioOutboxService, times(3))
                .enqueueAbortStagingIndependent(eq("lms-uploads"), anyString(), eq(1), isNull());
    }

    @Test
    void abortIfNoTransaction_isNoOpWhenSyncAlreadyRegistered() {
        S3UploadRollback rollback = new S3UploadRollback(minioOutboxService);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        S3UploadRollback.Scope scope = rollback.open(1, null);
        scope.remember("lms-uploads", "held.pdf");
        scope.abortIfNoTransaction();
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(any(), any(), any(), any());
    }
}
