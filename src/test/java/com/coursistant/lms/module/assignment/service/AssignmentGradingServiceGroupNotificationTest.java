package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.UpsertGradeRequest;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.entity.AssignmentGradeReleaseRecipient;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeReleaseRecipientMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionVersionMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupMembershipMapper;
import com.coursistant.lms.module.course.group.service.GroupAccessService;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentGradingServiceGroupNotificationTest {

    @Mock private AssignmentMapper assignmentMapper;
    @Mock private AssignmentGradeMapper assignmentGradeMapper;
    @Mock private AssignmentSubmissionMapper assignmentSubmissionMapper;
    @Mock private AssignmentSubmissionVersionMapper assignmentSubmissionVersionMapper;
    @Mock private AssignmentSubmissionFileMapper assignmentSubmissionFileMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private AssignmentAccessService assignmentAccessService;
    @Mock private AssignmentFilePolicy assignmentFilePolicy;
    @Mock private AssignmentStorageService assignmentStorageService;
    @Mock private AssignmentTimeSupport assignmentTimeSupport;
    @Mock private SubmissionStatusCalculator submissionStatusCalculator;
    @Mock private SubmissionResponseAssembler submissionResponseAssembler;
    @Mock private AssignmentRubricService assignmentRubricService;
    @Mock private AssignmentSubmissionService assignmentSubmissionService;
    @Mock private AssignmentResponseAssembler assignmentResponseAssembler;
    @Mock private AssignmentAuditService assignmentAuditService;
    @Mock private AssignmentNotificationService assignmentNotificationService;
    @Mock private AssignmentGradeReleaseRecipientMapper assignmentGradeReleaseRecipientMapper;
    @Mock private CourseGroupMapper courseGroupMapper;
    @Mock private GroupMembershipMapper groupMembershipMapper;
    @Mock private GroupAccessService groupAccessService;
    @Mock private TenantTimezoneService tenantTimezoneService;

    @InjectMocks
    private AssignmentGradingService assignmentGradingService;

    @Test
    void upsertGroupGrade_releasedCorrection_usesExistingIdSnapshotNotCurrentMembership() {
        Assignment assignment = groupAssignment();
        when(assignmentAccessService.requireGradingWritable(1, 20)).thenReturn(new Course());
        when(assignmentMapper.selectByCourseIdAndId(1, 9)).thenReturn(assignment);
        when(groupAccessService.requireGroupInSet(1, 5, 3)).thenReturn(new CourseGroup());

        AssignmentGrade existing = releasedGroupGrade(88);
        existing.setScore(new BigDecimal("5.0"));
        when(assignmentGradeMapper.selectByAssignmentIdAndGroupId(9, 3)).thenReturn(existing);
        when(assignmentSubmissionMapper.selectByAssignmentIdAndGroupId(9, 3)).thenReturn(null);
        when(assignmentTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(assignmentAuditService.write(eq(1), eq(9), eq(20), any(), anyMap())).thenReturn(11);
        when(assignmentAuditService.write(eq(1), eq(9), eq(20),
                eq(AssignmentAuditService.GRADE_CORRECTED_AFTER_RELEASE), anyMap())).thenReturn(77);

        AssignmentGradeReleaseRecipient snap = new AssignmentGradeReleaseRecipient();
        snap.setGradeId(88);
        snap.setStudentUserId(50);
        when(assignmentGradeReleaseRecipientMapper.selectByGradeId(88)).thenReturn(List.of(snap));

        UpsertGradeRequest body = new UpsertGradeRequest();
        body.setScore(new BigDecimal("8.0"));

        assignmentGradingService.upsertGroupGrade(1, 9, 3, 20, body);

        verify(assignmentGradeReleaseRecipientMapper).selectByGradeId(88);
        verify(groupMembershipMapper, never()).selectByGroupId(any());
        verify(assignmentNotificationService).recordGradeCorrectedAfterRelease(any(), eq(List.of(50)), eq(77));
    }

    @Test
    void upsertGroupGrade_emptySnapshot_stillNotifiesWithEmptyRecipients() {
        Assignment assignment = groupAssignment();
        when(assignmentAccessService.requireGradingWritable(1, 20)).thenReturn(new Course());
        when(assignmentMapper.selectByCourseIdAndId(1, 9)).thenReturn(assignment);
        when(groupAccessService.requireGroupInSet(1, 5, 3)).thenReturn(new CourseGroup());

        AssignmentGrade existing = releasedGroupGrade(88);
        existing.setScore(new BigDecimal("5.0"));
        when(assignmentGradeMapper.selectByAssignmentIdAndGroupId(9, 3)).thenReturn(existing);
        when(assignmentSubmissionMapper.selectByAssignmentIdAndGroupId(9, 3)).thenReturn(null);
        when(assignmentTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(assignmentAuditService.write(eq(1), eq(9), eq(20), any(), anyMap())).thenReturn(11);

        when(assignmentGradeReleaseRecipientMapper.selectByGradeId(88)).thenReturn(Collections.emptyList());

        UpsertGradeRequest body = new UpsertGradeRequest();
        body.setScore(new BigDecimal("8.0"));

        assignmentGradingService.upsertGroupGrade(1, 9, 3, 20, body);

        verify(assignmentNotificationService).recordGradeCorrectedAfterRelease(any(), eq(List.of()), any());
    }

    @Test
    void uploadGroupAnnotatedFile_releasedCorrection_usesExistingIdSnapshot() {
        Assignment assignment = groupAssignment();
        when(assignmentAccessService.requireGradingWritable(1, 20)).thenReturn(new Course());
        when(assignmentMapper.selectByCourseIdAndId(1, 9)).thenReturn(assignment);
        when(groupAccessService.requireGroupInSet(1, 5, 3)).thenReturn(new CourseGroup());

        AssignmentGrade existing = releasedGroupGrade(88);
        existing.setAnnotatedObjectKey("old-key");
        when(assignmentGradeMapper.selectByAssignmentIdAndGroupId(9, 3)).thenReturn(existing);

        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});
        when(assignmentFilePolicy.annotatedGroupKey(1, 9, 3, "a.pdf")).thenReturn("new-key");
        when(assignmentFilePolicy.sanitizeFilename("a.pdf")).thenReturn("a.pdf");
        when(assignmentAuditService.write(eq(1), eq(9), eq(20), any(), anyMap())).thenReturn(11);

        AssignmentGradeReleaseRecipient snap = new AssignmentGradeReleaseRecipient();
        snap.setStudentUserId(50);
        when(assignmentGradeReleaseRecipientMapper.selectByGradeId(88)).thenReturn(List.of(snap));

        assignmentGradingService.uploadGroupAnnotatedFile(1, 9, 3, 20, file);

        verify(assignmentGradeReleaseRecipientMapper).selectByGradeId(88);
        verify(groupMembershipMapper, never()).selectByGroupId(any());
        verify(assignmentNotificationService).recordGradeCorrectedAfterRelease(any(), eq(List.of(50)), any());
    }

    private static Assignment groupAssignment() {
        Assignment assignment = new Assignment();
        assignment.setId(9);
        assignment.setCourseId(1);
        assignment.setSubmissionType(AssignmentAccessService.SUBMISSION_TYPE_GROUP);
        assignment.setGroupSetId(5);
        assignment.setPointsPossible(new BigDecimal("10.0"));
        assignment.setTitle("Group HW");
        return assignment;
    }

    private static AssignmentGrade releasedGroupGrade(Integer id) {
        AssignmentGrade grade = new AssignmentGrade();
        grade.setId(id);
        grade.setAssignmentId(9);
        grade.setGroupId(3);
        grade.setStatus(AssignmentGradingService.GRADE_RELEASED);
        return grade;
    }
}
