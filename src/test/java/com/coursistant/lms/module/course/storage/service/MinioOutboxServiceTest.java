package com.coursistant.lms.module.course.storage.service;

import com.coursistant.lms.module.course.storage.entity.MinioObjectOutbox;
import com.coursistant.lms.module.course.storage.repository.MinioObjectOutboxMapper;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioOutboxServiceTest {

    @Mock private MinioObjectOutboxMapper minioObjectOutboxMapper;
    @Mock private S3ObjectStorage s3ObjectStorage;
    @Spy private S3ObjectKeyResolver s3ObjectKeyResolver = new S3ObjectKeyResolver();

    @InjectMocks
    private MinioOutboxService service;

    @Test
    void processBatch_deleteUsesResolvedKeyAndMarksDone() {
        MinioObjectOutbox row = row(1L, "lms-uploads", "staging/a.pdf", MinioObjectOutbox.ACTION_DELETE, 0);
        when(minioObjectOutboxMapper.selectClaimBatch(any(), eq(20))).thenReturn(List.of(row));
        when(minioObjectOutboxMapper.claim(eq(1L), any(), any())).thenReturn(1);

        service.processBatch();

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).deleteObject(key.capture());
        assertEquals("lms-uploads/staging/a.pdf", key.getValue());
        verify(minioObjectOutboxMapper).markDone(1L);
    }

    @Test
    void processBatch_idempotentDeleteStillDone() {
        MinioObjectOutbox row = row(2L, "lms-uploads", "gone.pdf", MinioObjectOutbox.ACTION_DELETE, 0);
        when(minioObjectOutboxMapper.selectClaimBatch(any(), anyInt())).thenReturn(List.of(row));
        when(minioObjectOutboxMapper.claim(eq(2L), any(), any())).thenReturn(1);

        service.processBatch();

        verify(s3ObjectStorage).deleteObject("lms-uploads/gone.pdf");
        verify(minioObjectOutboxMapper).markDone(2L);
    }

    @Test
    void processBatch_storageFailureRetries() {
        MinioObjectOutbox row = row(3L, "lms-uploads", "a.pdf", MinioObjectOutbox.ACTION_DELETE, 1);
        when(minioObjectOutboxMapper.selectClaimBatch(any(), anyInt())).thenReturn(List.of(row));
        when(minioObjectOutboxMapper.claim(eq(3L), any(), any())).thenReturn(1);
        doThrow(new S3StorageException("403")).when(s3ObjectStorage).deleteObject("lms-uploads/a.pdf");

        service.processBatch();

        verify(minioObjectOutboxMapper).markRetry(eq(3L), eq(2), any(LocalDateTime.class), eq("403"));
    }

    @Test
    void processBatch_maxRetriesMarksDead() {
        MinioObjectOutbox row = row(4L, "lms-uploads", "a.pdf", MinioObjectOutbox.ACTION_DELETE, 9);
        when(minioObjectOutboxMapper.selectClaimBatch(any(), anyInt())).thenReturn(List.of(row));
        when(minioObjectOutboxMapper.claim(eq(4L), any(), any())).thenReturn(1);
        doThrow(new S3StorageException("network")).when(s3ObjectStorage).deleteObject("lms-uploads/a.pdf");

        service.processBatch();

        verify(minioObjectOutboxMapper).markDead(4L, "network");
    }

    private static MinioObjectOutbox row(Long id, String bucket, String key, String action, int retries) {
        MinioObjectOutbox row = new MinioObjectOutbox();
        row.setId(id);
        row.setBucket(bucket);
        row.setObjectKey(key);
        row.setAction(action);
        row.setRetryCount(retries);
        return row;
    }
}
