package com.coursistant.lms.mapper.assignment;

import com.coursistant.lms.entity.GroupJoinRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GroupJoinRequestMapper {

    void insert(GroupJoinRequest request);

    void updateById(GroupJoinRequest request);

    void deleteById(Integer id);

    GroupJoinRequest selectById(Integer id);

    List<GroupJoinRequest> selectByGroupId(Integer groupId);

    List<GroupJoinRequest> selectByUserId(Integer userId);

    List<GroupJoinRequest> selectByAssignmentId(Integer assignmentId);

    List<GroupJoinRequest> selectByCourseId(Integer courseId);

    List<GroupJoinRequest> selectByStatus(String status);

    List<GroupJoinRequest> selectByGroupIdAndUserId(Integer groupId, Integer userId);

    List<GroupJoinRequest> selectByAssignmentIdAndUserId(Integer assignmentId, Integer userId);

    List<GroupJoinRequest> selectAll(GroupJoinRequest request);
}
