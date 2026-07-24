package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssignmentSubmissionReceiptMapper {

    int insert(AssignmentSubmissionReceipt receipt);

    AssignmentSubmissionReceipt selectById(@Param("id") Integer id);

    AssignmentSubmissionReceipt selectBySubmissionVersionId(@Param("submissionVersionId") Integer submissionVersionId);
}
