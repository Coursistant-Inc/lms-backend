package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.SubmissionResponse;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionStagingFileMapper;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionGetMySubmissionTest {

    @Mock private AssignmentAccessService assignmentAccessService;
    @Mock private AssignmentSubmissionMapper assignmentSubmissionMapper;
    @Mock private AssignmentSubmissionStagingFileMapper assignmentSubmissionStagingFileMapper;
    @Mock private AssignmentTimeSupport assignmentTimeSupport;
    @Mock private SubmissionStatusCalculator submissionStatusCalculator;
    @Mock private SubmissionResponseAssembler submissionResponseAssembler;
    @Mock private AssignmentFilePolicy assignmentFilePolicy;
    @Mock private TenantTimezoneService tenantTimezoneService;

    @InjectMocks
    private AssignmentSubmissionService service;

    @Test
    void getMySubmission_individualWithoutFormalVersion_returnsNotSubmittedShell() {
        Assignment assignment = new Assignment();
        assignment.setId(33);
        assignment.setCourseId(18);
        assignment.setSubmissionType(AssignmentAccessService.SUBMISSION_TYPE_INDIVIDUAL);
        assignment.setMaxFileCount(3);
        assignment.setMaxFileSizeBytes(1_000_000L);
        assignment.setDueAt(LocalDateTime.of(2026, 8, 20, 12, 0));
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 21, 47);

        when(assignmentAccessService.requireAssignmentReadable(any(), eq(18), eq(33), eq(385)))
                .thenReturn(assignment);
        when(tenantTimezoneService.requireZoneForCourse(18)).thenReturn(ZoneOffset.UTC);
        when(assignmentTimeSupport.nowUtc()).thenReturn(now);
        when(assignmentSubmissionMapper.selectByAssignmentIdAndOwnerUserId(33, 385)).thenReturn(null);
        when(assignmentSubmissionStagingFileMapper.selectByAssignmentIdAndOwnerUserIdAndNotConsumed(33, 385))
                .thenReturn(List.of());
        when(assignmentFilePolicy.parseAllowedTypes(any())).thenReturn(List.of("pdf"));
        when(submissionResponseAssembler.toStagingResponses(any())).thenReturn(List.of());
        when(submissionResponseAssembler.toVersionResponse(eq(assignment), isNull(), any(), eq(true)))
                .thenReturn(null);
        when(submissionStatusCalculator.isWindowOpen(any(), any(), any())).thenReturn(true);
        when(submissionStatusCalculator.calculate(any(), any(), any(), isNull(), isNull(), any()))
                .thenReturn(SubmissionStatusCalculator.NOT_SUBMITTED);
        when(submissionStatusCalculator.isGraceEligible(any(), any(), any(), any())).thenReturn(false);
        when(submissionStatusCalculator.acceptSubmit(any(), any(), any(), any())).thenReturn(true);
        when(assignmentAccessService.isSubmitFrozen(18, 385)).thenReturn(false);

        SubmissionResponse response = service.getMySubmission(new MockHttpServletRequest(), 18, 33, 385);

        assertEquals(33, response.getAssignmentId());
        assertEquals(385, response.getOwnerUserId());
        assertNull(response.getSubmissionId());
        assertNull(response.getCurrentVersion());
        assertEquals(0, response.getTotalVersions());
        assertEquals(SubmissionStatusCalculator.NOT_SUBMITTED, response.getSubmissionStatus());
    }
}
