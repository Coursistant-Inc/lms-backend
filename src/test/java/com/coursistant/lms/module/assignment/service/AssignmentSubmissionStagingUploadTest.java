package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.StagingFileResponse;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionStagingFileMapper;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import com.coursistant.lms.module.file.storage.S3UploadRollback;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionStagingUploadTest {

    @Mock private AssignmentMapper assignmentMapper;
    @Mock private AssignmentSubmissionStagingFileMapper assignmentSubmissionStagingFileMapper;
    @Mock private AssignmentAccessService assignmentAccessService;
    @Mock private AssignmentFilePolicy assignmentFilePolicy;
    @Mock private AssignmentStorageService assignmentStorageService;
    @Mock private AssignmentTimeSupport assignmentTimeSupport;
    @Mock private SubmissionStatusCalculator submissionStatusCalculator;
    @Mock private SubmissionResponseAssembler submissionResponseAssembler;
    @Mock private MinioOutboxService minioOutboxService;

    @InjectMocks
    private AssignmentSubmissionService service;

    @BeforeEach
    void injectRollback() {
        ReflectionTestUtils.setField(service, "s3UploadRollback", new S3UploadRollback(minioOutboxService));
        org.mockito.Mockito.lenient().when(assignmentFilePolicy.bucket()).thenReturn("lms-uploads");
    }

    @Test
    void orphanV1_secondFileFailsValidation_neverPuts() {
        stubPublishedAssignment();
        MockMultipartFile file1 = png("a.png");
        MockMultipartFile file2 = png("b.png");
        when(assignmentFilePolicy.validateSubmissionFile(eq(file1), anyList(), anyLong()))
                .thenReturn("image/png");
        when(assignmentFilePolicy.validateSubmissionFile(eq(file2), anyList(), anyLong()))
                .thenThrow(new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.uploadStagingFiles(1, 2, 3, new MockMultipartFile[]{file1, file2}));
        assertEquals(ErrorType.UNSUPPORTED_FILE_TYPE, ex.getErrorType());
        verify(assignmentStorageService, never()).upload(any(), any(), any(), any(), any(), any());
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(any(), any(), any(), any());
    }

    @Test
    void orphanV2_secondPutFails_abortsFirstLogicalKey() {
        stubPublishedAssignment();
        MockMultipartFile file1 = png("a.png");
        MockMultipartFile file2 = png("b.png");
        when(assignmentFilePolicy.validateSubmissionFile(any(), anyList(), anyLong())).thenReturn("image/png");
        when(assignmentFilePolicy.stagingKey(1, 2, 3, "a.png")).thenReturn("k1");
        when(assignmentFilePolicy.stagingKey(1, 2, 3, "b.png")).thenReturn("k2");
        when(assignmentFilePolicy.checksumSha256(any())).thenReturn("abc");
        org.mockito.Mockito.doAnswer(invocation -> {
            if ("k2".equals(invocation.getArgument(0))) {
                throw new ApiException(ErrorType.STORAGE_FAILURE);
            }
            return null;
        }).when(assignmentStorageService).upload(any(), any(), any(), any(), any(), any());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.uploadStagingFiles(1, 2, 3, new MockMultipartFile[]{file1, file2}));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        verify(assignmentStorageService).upload(eq("k1"), eq(file1), eq("image/png"), eq(1), eq(2), eq(3));
        verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "k1", 1, null);
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(
                eq("lms-uploads"), eq("k2"), anyInt(), any());
    }

    private void stubPublishedAssignment() {
        Assignment assignment = new Assignment();
        assignment.setId(2);
        assignment.setCourseId(1);
        assignment.setState(AssignmentAccessService.STATE_PUBLISHED);
        assignment.setMaxFileCount(10);
        assignment.setMaxFileSizeBytes(1_000_000L);
        assignment.setAllowedFileTypes("[\"png\"]");
        assignment.setDueAt(LocalDateTime.now().plusDays(1));
        when(assignmentMapper.selectByCourseIdAndId(1, 2)).thenReturn(assignment);
        when(assignmentTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 17, 12, 0));
        when(assignmentSubmissionStagingFileMapper.selectByAssignmentIdAndOwnerUserIdAndNotConsumed(2, 3))
                .thenReturn(List.of());
        when(assignmentFilePolicy.parseAllowedTypes(any())).thenReturn(List.of("png"));
        when(submissionStatusCalculator.acceptSubmit(any(), any(), any(), any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(submissionResponseAssembler.toStagingResponse(any()))
                .thenReturn(new StagingFileResponse());
        org.mockito.Mockito.lenient().when(assignmentFilePolicy.sanitizeFilename(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static MockMultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", new byte[]{1, 2, 3});
    }
}
