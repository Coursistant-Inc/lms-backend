package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import com.coursistant.lms.module.assignment.repository.AssignmentGradeMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.service.GroupAccessService;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import com.coursistant.lms.module.file.storage.S3UploadRollback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentGradingAnnotatedUploadTest {

    @Mock private AssignmentAccessService assignmentAccessService;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private AssignmentGradeMapper assignmentGradeMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private AssignmentFilePolicy assignmentFilePolicy;
    @Mock private AssignmentStorageService assignmentStorageService;
    @Mock private AssignmentAuditService assignmentAuditService;
    @Mock private AssignmentNotificationService assignmentNotificationService;
    @Mock private AssignmentResponseAssembler assignmentResponseAssembler;
    @Mock private AssignmentTimeSupport assignmentTimeSupport;
    @Mock private GroupAccessService groupAccessService;
    @Mock private MinioOutboxService minioOutboxService;

    @InjectMocks
    private AssignmentGradingService service;

    @BeforeEach
    void injectRollback() {
        ReflectionTestUtils.setField(service, "s3UploadRollback", new S3UploadRollback(minioOutboxService));
        org.mockito.Mockito.lenient().when(assignmentFilePolicy.bucket()).thenReturn("lms-uploads");
        org.mockito.Mockito.lenient().when(assignmentFilePolicy.sanitizeFilename(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(assignmentResponseAssembler.absoluteUrl(anyString()))
                .thenReturn("/api/x");
    }

    @Test
    void orphanV7_replacingAnnotatedFile_enqueuesDeleteNotQuietDelete() {
        Assignment assignment = assignment(1, 2);
        when(assignmentMapper.selectByCourseIdAndId(1, 2)).thenReturn(assignment);
        when(enrollmentMapper.selectByCourseIdAndUserId(1, 9)).thenReturn(student());
        AssignmentGrade grade = grade(5, "old-ann.pdf");
        when(assignmentGradeMapper.selectByAssignmentIdAndStudentUserId(2, 9)).thenReturn(grade);
        when(assignmentFilePolicy.validateAnnotatedFile(any())).thenReturn("application/pdf");
        when(assignmentFilePolicy.annotatedKey(1, 2, 9, "new.pdf")).thenReturn("new-ann.pdf");

        service.uploadAnnotatedFile(1, 2, 9, 4, pdf("new.pdf"));

        verify(minioOutboxService).enqueueDelete("lms-uploads", "old-ann.pdf", 1, null);
        verify(assignmentStorageService, never()).deleteQuietly(any());
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(any(), any(), any(), any());
    }

    @Test
    void orphanV7_replacingGroupAnnotatedFile_enqueuesDeleteNotQuietDelete() {
        Assignment assignment = assignment(1, 2);
        assignment.setSubmissionType(AssignmentAccessService.SUBMISSION_TYPE_GROUP);
        assignment.setGroupSetId(7);
        when(assignmentMapper.selectByCourseIdAndId(1, 2)).thenReturn(assignment);
        when(groupAccessService.requireGroupInSet(1, 7, 3)).thenReturn(new CourseGroup());
        AssignmentGrade grade = grade(5, "old-group.pdf");
        grade.setGroupId(3);
        when(assignmentGradeMapper.selectByAssignmentIdAndGroupId(2, 3)).thenReturn(grade);
        when(assignmentFilePolicy.validateAnnotatedFile(any())).thenReturn("application/pdf");
        when(assignmentFilePolicy.annotatedGroupKey(1, 2, 3, "new.pdf")).thenReturn("new-group.pdf");

        service.uploadGroupAnnotatedFile(1, 2, 3, 4, pdf("new.pdf"));

        verify(minioOutboxService).enqueueDelete("lms-uploads", "old-group.pdf", 1, null);
        verify(assignmentStorageService, never()).deleteQuietly(any());
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(any(), any(), any(), any());
    }

    private static Assignment assignment(int courseId, int assignmentId) {
        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setCourseId(courseId);
        assignment.setState(AssignmentAccessService.STATE_PUBLISHED);
        return assignment;
    }

    private static AssignmentGrade grade(int id, String previousKey) {
        AssignmentGrade grade = new AssignmentGrade();
        grade.setId(id);
        grade.setAssignmentId(2);
        grade.setStudentUserId(9);
        grade.setStatus(AssignmentGradingService.GRADE_ENTERED);
        grade.setAnnotatedObjectKey(previousKey);
        return grade;
    }

    private static Enrollment student() {
        Enrollment enrollment = new Enrollment();
        enrollment.setActive(true);
        enrollment.setCourseRole(CoursePermissionService.ROLE_STUDENT);
        return enrollment;
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[]{1});
    }
}
