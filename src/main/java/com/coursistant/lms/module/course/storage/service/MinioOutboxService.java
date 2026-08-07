package com.coursistant.lms.module.course.storage.service;

import com.coursistant.lms.module.course.storage.entity.MinioObjectOutbox;
import com.coursistant.lms.module.course.storage.repository.MinioObjectOutboxMapper;
import com.coursistant.lms.module.file.service.MinIOService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MinioOutboxService {

    private static final Logger log = LoggerFactory.getLogger(MinioOutboxService.class);
    private static final int MAX_RETRIES = 10;
    private static final int BATCH = 20;

    @Resource
    private MinioObjectOutboxMapper minioObjectOutboxMapper;

    @Resource
    private MinIOService minIOService;

    @Transactional
    public void enqueueDelete(String bucket, String objectKey, Integer courseId, String uploadOperationId) {
        MinioObjectOutbox row = newPending(bucket, objectKey, MinioObjectOutbox.ACTION_DELETE, courseId, uploadOperationId);
        minioObjectOutboxMapper.insert(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueAbortStagingIndependent(String bucket, String objectKey, Integer courseId, String uploadOperationId) {
        MinioObjectOutbox row = newPending(bucket, objectKey, MinioObjectOutbox.ACTION_ABORT_STAGING, courseId, uploadOperationId);
        minioObjectOutboxMapper.insert(row);
    }

    @Scheduled(fixedDelayString = "${lms.minio-outbox.poll-ms:5000}")
    @Transactional
    public void processBatch() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<MinioObjectOutbox> batch = minioObjectOutboxMapper.selectClaimBatch(now, BATCH);
        for (MinioObjectOutbox row : batch) {
            LocalDateTime leaseUntil = now.plusMinutes(2);
            int claimed = minioObjectOutboxMapper.claim(row.getId(), leaseUntil, now);
            if (claimed == 0) {
                continue;
            }
            try {
                if (MinioObjectOutbox.ACTION_DELETE.equals(row.getAction())
                        || MinioObjectOutbox.ACTION_ABORT_STAGING.equals(row.getAction())) {
                    try {
                        minIOService.deleteFile(row.getObjectKey(), row.getBucket());
                    } catch (Exception e) {
                        // Object already gone is success for DELETE/ABORT.
                        if (!isNotFound(e)) {
                            throw e;
                        }
                    }
                } else if (MinioObjectOutbox.ACTION_COMMIT_OBJECT.equals(row.getAction())) {
                    // COMMIT is normally done inline; worker only cleans/retries recorded commits.
                    log.debug("COMMIT_OBJECT worker noop for key={}", row.getObjectKey());
                }
                minioObjectOutboxMapper.markDone(row.getId());
            } catch (Exception e) {
                int next = (row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1;
                String err = truncate(e.getMessage());
                if (next >= MAX_RETRIES) {
                    log.error("MinIO outbox DEAD id={} action={} key={}: {}",
                            row.getId(), row.getAction(), row.getObjectKey(), err);
                    minioObjectOutboxMapper.markDead(row.getId(), err);
                } else {
                    LocalDateTime nextAt = now.plusSeconds((long) Math.min(3600, Math.pow(2, next)));
                    minioObjectOutboxMapper.markRetry(row.getId(), next, nextAt, err);
                }
            }
        }
    }

    private static MinioObjectOutbox newPending(String bucket, String objectKey, String action,
                                                Integer courseId, String uploadOperationId) {
        MinioObjectOutbox row = new MinioObjectOutbox();
        row.setBucket(bucket);
        row.setObjectKey(objectKey);
        row.setAction(action);
        row.setStatus(MinioObjectOutbox.STATUS_PENDING);
        row.setRetryCount(0);
        row.setNextRetryAt(LocalDateTime.now(ZoneOffset.UTC));
        row.setCourseId(courseId);
        row.setUploadOperationId(uploadOperationId);
        return row;
    }

    private static boolean isNotFound(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("NoSuchKey") || msg.contains("Not Found") || msg.contains("404"));
    }

    private static String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
