package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.dto.StagingFileResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionFileResponse;
import com.coursistant.lms.module.assignment.dto.SubmissionVersionResponse;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionStagingFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionVersion;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionReceiptMapper;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionReceipt;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Shapes submission versions, submission files, and staging files. Statuses are always derived
 * through {@link SubmissionStatusCalculator}, never read from a column.
 */
@Component
public class SubmissionResponseAssembler {

    @Resource
    private AssignmentSubmissionFileMapper assignmentSubmissionFileMapper;

    @Resource
    private AssignmentSubmissionReceiptMapper assignmentSubmissionReceiptMapper;

    @Resource
    private AssignmentFilePolicy assignmentFilePolicy;

    @Resource
    private AssignmentResponseAssembler assignmentResponseAssembler;

    @Resource
    private AssignmentTimeSupport assignmentTimeSupport;

    @Resource
    private SubmissionStatusCalculator submissionStatusCalculator;

    public SubmissionVersionResponse toVersionResponse(Assignment assignment,
                                                       AssignmentSubmissionVersion version,
                                                       ZoneId zone,
                                                       boolean includeFiles) {
        if (version == null) {
            return null;
        }
        SubmissionVersionResponse response = new SubmissionVersionResponse();
        response.setId(version.getId());
        response.setSubmissionId(version.getSubmissionId());
        response.setAssignmentId(version.getAssignmentId());
        response.setOwnerUserId(version.getOwnerUserId());
        response.setVersionNo(version.getVersionNo());
        response.setSubmittedAt(version.getSubmittedAt());
        response.setSubmittedAtLocal(assignmentTimeSupport.toZone(version.getSubmittedAt(), zone));
        response.setUsedGraceBuffer(version.getUsedGraceBuffer());
        response.setSubmissionStatus(submissionStatusCalculator.calculateForVersion(
                assignment.getDueAt(), version.getSubmittedAt(), version.getUsedGraceBuffer()));

        List<AssignmentSubmissionFile> files = assignmentSubmissionFileMapper.selectBySubmissionVersionId(version.getId());
        response.setFileCount(files == null ? 0 : files.size());
        if (includeFiles) {
            response.setFiles(toFileResponses(assignment.getCourseId(), assignment.getId(), version, files));
        }

        AssignmentSubmissionReceipt receipt = assignmentSubmissionReceiptMapper.selectBySubmissionVersionId(version.getId());
        if (receipt != null) {
            response.setReceiptIssuedAt(receipt.getIssuedAt());
        }
        return response;
    }

    public List<SubmissionFileResponse> toFileResponses(Integer courseId, Integer assignmentId,
                                                        AssignmentSubmissionVersion version,
                                                        List<AssignmentSubmissionFile> files) {
        List<SubmissionFileResponse> result = new ArrayList<>();
        if (files == null) {
            return result;
        }
        for (AssignmentSubmissionFile file : files) {
            result.add(toFileResponse(courseId, assignmentId, version, file));
        }
        return result;
    }

    public SubmissionFileResponse toFileResponse(Integer courseId, Integer assignmentId,
                                                 AssignmentSubmissionVersion version,
                                                 AssignmentSubmissionFile file) {
        SubmissionFileResponse response = new SubmissionFileResponse();
        response.setId(file.getId());
        response.setSubmissionVersionId(file.getSubmissionVersionId());
        response.setOriginalName(file.getOriginalName());
        response.setContentType(file.getContentType());
        response.setSizeBytes(file.getSizeBytes());
        response.setChecksumSha256(file.getChecksumSha256());
        response.setSortOrder(file.getSortOrder());
        response.setCreatedAt(file.getCreatedAt());

        String base = "/v2/courses/" + courseId + "/assignments/" + assignmentId
                + "/submissions/" + version.getSubmissionId() + "/files/" + file.getId();
        response.setDownloadUrl(assignmentResponseAssembler.absoluteUrl(base + "/download"));
        boolean previewable = assignmentFilePolicy.isPreviewable(file.getContentType(), file.getOriginalName());
        response.setPreviewAvailable(previewable);
        if (previewable) {
            response.setPreviewUrl(assignmentResponseAssembler.absoluteUrl(base + "/preview"));
        }
        return response;
    }

    public List<StagingFileResponse> toStagingResponses(List<AssignmentSubmissionStagingFile> stagingFiles) {
        List<StagingFileResponse> result = new ArrayList<>();
        if (stagingFiles == null) {
            return result;
        }
        for (AssignmentSubmissionStagingFile stagingFile : stagingFiles) {
            result.add(toStagingResponse(stagingFile));
        }
        return result;
    }

    public StagingFileResponse toStagingResponse(AssignmentSubmissionStagingFile stagingFile) {
        StagingFileResponse response = new StagingFileResponse();
        response.setId(stagingFile.getId());
        response.setAssignmentId(stagingFile.getAssignmentId());
        response.setOriginalName(stagingFile.getOriginalName());
        response.setContentType(stagingFile.getContentType());
        response.setSizeBytes(stagingFile.getSizeBytes());
        response.setChecksumSha256(stagingFile.getChecksumSha256());
        response.setCreatedAt(stagingFile.getCreatedAt());
        response.setExpiresAt(stagingFile.getExpiresAt());
        return response;
    }
}
