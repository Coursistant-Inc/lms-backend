package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.assignment.entity.GroupMember;
import com.coursistant.lms.module.assignment.service.GroupMemberService;

import cn.hutool.core.util.ObjectUtil;

import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 小组成员控制器
* Controller for group members
*/
@RestController
@RequestMapping("/groupMember")
public class GroupMemberController {

    @Resource
    private GroupMemberService groupMemberService;

    //private static final Logger logger = Logger.getLogger(GroupMemberController.class.getName());


    @DeleteMapping("/deleteByGroup/{groupId}")
    public Result deleteByGroup(@PathVariable Integer groupId) {
        groupMemberService.deleteByGroupId(groupId);
        return Result.success();
    }

    @GetMapping("/selectByGroup/{groupId}")
    public Result selectByGroup(@PathVariable Integer groupId) {
        List<GroupMember> list = groupMemberService.selectByGroupId(groupId);
        return Result.success(list);
    }


    @PostMapping("/addMemberById")
    public Result addMemberByEmail(@RequestBody Map<String, Object> params) {
        Integer groupId = (Integer) params.get("groupId");
        Integer userId = (Integer) params.get("userId");

        if (ObjectUtil.isEmpty(groupId) || ObjectUtil.isEmpty(userId)) {
            return Result.error("400", "Missing groupId or email");
        }

        groupMemberService.addMemberById(groupId, userId);
        return Result.success("Member added successfully");
    }

    @DeleteMapping("/selfRemove")
    public Result selfRemove(@RequestBody Map<String, Object> params) {
        Integer groupId = (Integer) params.get("groupId");
        Integer userId = (Integer) params.get("userId"); 
        GroupMember member = groupMemberService.selectByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            return Result.success("You have already left the group.");
        }
        groupMemberService.deleteByGroupIdAndUserId(groupId, userId);
        return Result.success("You have left the group.");
    }

    @DeleteMapping("/removeMember")
    public Result removeMember(@RequestBody Map<String, Object> params) {
        Integer groupId = (Integer) params.get("groupId");
        Integer userId = (Integer) params.get("userId");
        GroupMember member = groupMemberService.selectByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            return Result.success("This member is not in the group.");
        }
        groupMemberService.deleteByGroupIdAndUserId(groupId, userId);
        return Result.success("Member removed.");
    }

    @GetMapping("/selectWithCount/{groupId}")
    public Result selectWithCount(@PathVariable Integer groupId) {
        List<GroupMember> list = groupMemberService.selectByGroupId(groupId);
        int count = list.size();

        Map<String, Object> result = new HashMap<>();
        result.put("members", list);
        result.put("count", count);

        return Result.success(result);
    }



}