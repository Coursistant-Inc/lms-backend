package com.coursistant.lms.module.course.storage.service;

import com.coursistant.lms.module.course.storage.entity.UploadOperation;
import com.coursistant.lms.module.course.storage.repository.UploadOperationMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UploadOperationService {

    @Resource
    private UploadOperationMapper uploadOperationMapper;

    @Transactional
    public UploadOperation createOrResume(ActorContext actor, String idempotencyKey, String routeId,
                                          String fingerprint, Integer courseId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorType.IDEMPOTENCY_KEY_REQUIRED);
        }
        UploadOperation existing = uploadOperationMapper.selectByActorKeyRoute(
                actor.getActorType(), actor.getActorId(), idempotencyKey.trim(), routeId);
        if (existing != null) {
            if (!fingerprint.equals(existing.getFingerprint())) {
                throw new ApiException(ErrorType.IDEMPOTENCY_KEY_MISMATCH);
            }
            return existing;
        }
        UploadOperation op = new UploadOperation();
        op.setId(UUID.randomUUID().toString());
        op.setActorType(actor.getActorType());
        op.setActorId(actor.getActorId());
        op.setIdempotencyKey(idempotencyKey.trim());
        op.setRouteId(routeId);
        op.setFingerprint(fingerprint);
        op.setStatus(UploadOperation.STATUS_PENDING);
        op.setVisibilityStatus(UploadOperation.STATUS_PENDING);
        op.setCourseId(courseId);
        uploadOperationMapper.insert(op);
        return op;
    }

    @Transactional
    public void markReady(String operationId) {
        uploadOperationMapper.updateStatus(operationId, UploadOperation.STATUS_READY, UploadOperation.STATUS_READY);
    }

    @Transactional
    public void markFailed(String operationId) {
        uploadOperationMapper.updateStatus(operationId, UploadOperation.STATUS_FAILED, UploadOperation.STATUS_FAILED);
    }
}
