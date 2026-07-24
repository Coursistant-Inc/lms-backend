package com.coursistant.lms.module.course.group.repository;

import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMembershipAuditMapper {

    int insert(GroupMembershipAudit audit);

    List<GroupMembershipAudit> selectByCourseId(@Param("courseId") Integer courseId);

    List<GroupMembershipAudit> selectByGroupSetId(@Param("groupSetId") Integer groupSetId);

    int countByGroupSetIdAndTargetUserId(@Param("groupSetId") Integer groupSetId,
                                         @Param("targetUserId") Integer targetUserId);
}
