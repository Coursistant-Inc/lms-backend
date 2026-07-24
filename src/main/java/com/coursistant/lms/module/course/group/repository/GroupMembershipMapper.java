package com.coursistant.lms.module.course.group.repository;

import com.coursistant.lms.module.course.group.entity.GroupMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMembershipMapper {

    int insert(GroupMembership membership);

    int updateById(GroupMembership membership);

    int deleteById(@Param("id") Integer id);

    int deleteByCourseIdAndUserId(@Param("courseId") Integer courseId, @Param("userId") Integer userId);

    GroupMembership selectById(@Param("id") Integer id);

    GroupMembership selectByGroupSetIdAndUserId(@Param("groupSetId") Integer groupSetId,
                                                @Param("userId") Integer userId);

    List<GroupMembership> selectByGroupId(@Param("groupId") Integer groupId);

    List<GroupMembership> selectByGroupSetId(@Param("groupSetId") Integer groupSetId);

    List<GroupMembership> selectByCourseIdAndUserId(@Param("courseId") Integer courseId,
                                                    @Param("userId") Integer userId);

    int countByGroupId(@Param("groupId") Integer groupId);

    int countByGroupSetId(@Param("groupSetId") Integer groupSetId);
}
