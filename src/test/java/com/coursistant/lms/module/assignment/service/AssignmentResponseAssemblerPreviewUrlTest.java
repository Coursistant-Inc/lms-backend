package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.AssignmentAttachmentResponse;
import com.coursistant.lms.module.assignment.dto.RubricResponse;
import com.coursistant.lms.module.assignment.entity.AssignmentAttachment;
import com.coursistant.lms.module.assignment.entity.AssignmentRubricVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AssignmentResponseAssemblerPreviewUrlTest {

    @Spy
    private AssignmentFilePolicy assignmentFilePolicy = new AssignmentFilePolicy();

    @Mock
    private AssignmentTimeSupport assignmentTimeSupport;

    @InjectMocks
    private AssignmentResponseAssembler assembler;

    @Test
    void pdfAttachment_includesPreviewUrl() {
        AssignmentAttachment attachment = attachment(16, 48, "brief.pdf", "application/pdf");

        AssignmentAttachmentResponse response = assembler.toAttachmentResponse(34, attachment);

        assertTrue(response.isPreviewAvailable());
        assertTrue(response.getDownloadUrl().endsWith(
                "/v2/courses/34/assignments/48/attachments/16/download"));
        assertTrue(response.getPreviewUrl().endsWith(
                "/v2/courses/34/assignments/48/attachments/16/preview"));
    }

    @Test
    void zipAttachment_omitsPreviewUrl() {
        AssignmentAttachment attachment = attachment(17, 48, "starter.zip", "application/zip");

        AssignmentAttachmentResponse response = assembler.toAttachmentResponse(34, attachment);

        assertFalse(response.isPreviewAvailable());
        assertNull(response.getPreviewUrl());
        assertTrue(response.getDownloadUrl().endsWith(
                "/v2/courses/34/assignments/48/attachments/17/download"));
    }

    @Test
    void postedRubricPdf_includesPreviewUrl() {
        AssignmentRubricVersion version = new AssignmentRubricVersion();
        version.setId(25);
        version.setOriginalName("rubric.pdf");
        version.setContentType("application/pdf");

        RubricResponse response = assembler.toRubricResponse(34, 48, version, 1, false);

        assertTrue(response.isPosted());
        assertTrue(response.isPreviewAvailable());
        assertTrue(response.getDownloadUrl().endsWith("/v2/courses/34/assignments/48/rubric/download"));
        assertTrue(response.getPreviewUrl().endsWith("/v2/courses/34/assignments/48/rubric/preview"));
    }

    @Test
    void unpostedRubric_hasNoFileUrls() {
        RubricResponse response = assembler.toRubricResponse(34, 48, null, 0, false);

        assertFalse(response.isPosted());
        assertFalse(response.isPreviewAvailable());
        assertNull(response.getDownloadUrl());
        assertNull(response.getPreviewUrl());
        assertEquals(48, response.getAssignmentId());
    }

    private static AssignmentAttachment attachment(int id, int assignmentId, String name, String contentType) {
        AssignmentAttachment attachment = new AssignmentAttachment();
        attachment.setId(id);
        attachment.setAssignmentId(assignmentId);
        attachment.setOriginalName(name);
        attachment.setContentType(contentType);
        return attachment;
    }
}
