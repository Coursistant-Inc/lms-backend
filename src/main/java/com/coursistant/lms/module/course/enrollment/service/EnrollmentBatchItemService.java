package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-item student enroll with independent commit (Part 3 batch partial success).
 * Must be a separate Spring bean — never invoked via self-call.
 */
@Service
public class EnrollmentBatchItemService {

    @Lazy
    @Resource
    private EnrollmentMembershipService enrollmentMembershipService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MemberResponse addStudentItem(ActorContext actor, Integer courseId, Integer userId, String requestId) {
        return enrollmentMembershipService.upsertStudentLocked(actor, courseId, userId, requestId);
    }
}
