package com.coursistant.lms.controller.assignment;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.GroupMember;
import com.coursistant.lms.service.assignment.GroupMemberService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(GroupMemberController.class.getName());



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


    // @PostMapping("/addByEmail")
    // public Result addByEmail(@RequestBody Map<String, Object> params) {
    //     try {
    //         Integer groupId = (Integer) params.get("groupId");
    //         String email = (String) params.get("email");

    //         if (groupId == null || email == null || email.trim().isEmpty()) {
    //             return Result.error("400", "Missing groupId or email");
    //         }

    //         groupMemberService.addMemberByEmail(groupId, email);
    //         return Result.success("Member added successfully");

    //     } catch (RuntimeException e) {
    //         String message = e.getMessage();

    //         // 你可以根据 message 的内容细分错误类型
    //         if (message.contains("not found")) {
    //             return Result.error("404", message);  // Not Found
    //         } else if (message.contains("already in another group")) {
    //             return Result.error("409", message);  // Conflict
    //         } else if (message.contains("not enrolled")) {
    //             return Result.error("403", message);  // Forbidden
    //         } else if (message.contains("full")) {
    //             return Result.error("400", message);  // Bad Request
    //         } else {
    //             return Result.error("500", "Unknown error: " + message);
    //         }
    //     }
    // }


    @DeleteMapping("/selfRemove")
    public Result selfRemove(@RequestBody Map<String, Object> params) {
        Integer groupId = (Integer) params.get("groupId");
        // 你项目中 userId 通常从 token 拿，我们这里暂时硬编码 or 传进来
        Integer userId = (Integer) params.get("userId");  // TODO: 改成从登录用户获取
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