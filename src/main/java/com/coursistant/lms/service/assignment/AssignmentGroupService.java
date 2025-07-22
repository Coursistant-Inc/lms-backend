package com.coursistant.lms.service.assignment;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentGroup;
import com.coursistant.lms.mapper.assignment.AssignmentGroupMapper;

import cn.hutool.core.util.ObjectUtil;

import com.coursistant.lms.entity.GroupMember;
import com.coursistant.lms.entity.User;
import com.coursistant.lms.exception.CustomException;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AssignmentGroupService {

    @Resource
    private AssignmentGroupMapper assignmentGroupMapper;

    @Resource
    private com.coursistant.lms.mapper.assignment.GroupMemberMapper groupMemberMapper;

    @Resource
    private com.coursistant.lms.mapper.assignment.AssignmentMapper assignmentMapper;

    @Resource
    private com.coursistant.lms.mapper.user.UserMapper userMapper;

    /**
     * 新增小组
    */
    public void add(AssignmentGroup group) {
        assignmentGroupMapper.insert(group);
    }

    /**
     * 更新小组
    */
    public void updateById(AssignmentGroup group) {
        assignmentGroupMapper.updateById(group);
    }

    /**
     * 删除小组
    */
    public void deleteById(Integer id) {
        assignmentGroupMapper.deleteById(id);
    }

    /**
     * 查询小组
    */
    public AssignmentGroup selectById(Integer id) {
        return assignmentGroupMapper.selectById(id);
    }

    /**
     * 查询所有（可带条件）
    */
    public List<AssignmentGroup> selectAll(AssignmentGroup group) {
        return assignmentGroupMapper.selectAll(group);
    }

    /**
     * 查询某作业下的所有小组
    */
    public List<AssignmentGroup> selectByAssignmentId(Integer assignmentId) {
        return assignmentGroupMapper.selectByAssignmentId(assignmentId);
    }


    /**
     * 自动分组功能
    */
    public void autoGroup(Integer assignmentId, Integer groupSize) {

        // 清除旧分组数据
        List<AssignmentGroup> oldGroups = assignmentGroupMapper.selectByAssignmentId(assignmentId);
        for (AssignmentGroup group : oldGroups) {
            groupMemberMapper.deleteByGroupId(group.getId());     // 删除旧成员
            assignmentGroupMapper.deleteById(group.getId());       // 删除旧小组
        }


        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (ObjectUtil.isNull(assignment)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(assignment.getCourseId())) {
            throw new CustomException(ResultCodeEnum.COURSE_NOT_EXIST_ERROR);
        }
        assignment.setGroupSize(groupSize);
        assignmentMapper.updateById(assignment);
        Integer courseId = assignment.getCourseId();

        // 构造条件：只查学生
        List<User> students = userMapper.selectStudentsByCourseId(courseId);


        java.util.Collections.shuffle(students); // 打乱顺序

        int index = 0;
        int total = students.size();
        int groupCount = (int) Math.ceil((double) total / groupSize);

        for (int i = 0; i < groupCount; i++) {
            AssignmentGroup group = new AssignmentGroup();
            group.setAssignmentId(assignmentId);
            group.setCourseId(courseId);
            this.add(group);  // 调用已有方法

            for (int j = 0; j < groupSize && index < total; j++, index++) {
                User student = students.get(index);
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setCourseId(courseId);
                member.setUserId(student.getId());
                groupMemberMapper.insert(member);
            }
        }
    }


}
