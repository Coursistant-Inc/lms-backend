package com.coursistant.lms.module.assignment.repository;


import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMemberMapper {

    void insert(GroupMember member);

    void deleteById(Integer id);

    void deleteByGroupId(Integer groupId); // 删除整组

    List<GroupMember> selectByGroupId(Integer groupId);

    GroupMember selectByGroupIdAndUserId(Integer groupId, Integer userId);

    List<GroupMember> selectAll(GroupMember member);


    Integer selectGroupIdByCourseIdAndUserId(@Param("courseId") Integer courseId,
                                             @Param("userId") Integer userId);
}