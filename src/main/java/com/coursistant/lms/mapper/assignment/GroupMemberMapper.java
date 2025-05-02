package com.coursistant.lms.mapper.assignment;


import com.coursistant.lms.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GroupMemberMapper {

    void insert(GroupMember member);

    void deleteById(Integer id);

    void deleteByGroupId(Integer groupId); // 删除整组

    List<GroupMember> selectByGroupId(Integer groupId);

    GroupMember selectByGroupIdAndUserId(Integer groupId, Integer userId);
}