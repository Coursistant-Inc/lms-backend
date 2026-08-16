package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.AccountStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationContactLookup {

    @Resource
    private UserMapper userMapper;

    public Map<Integer, User> load(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectNotificationContactsByIds(userIds);
        Map<Integer, User> map = new HashMap<>();
        if (users != null) {
            for (User user : users) {
                if (user != null && user.getId() != null) {
                    map.put(user.getId(), user);
                }
            }
        }
        return map;
    }

    public boolean emailEnabled(User user) {
        return user != null && (user.getEmailNotifications() == null || user.getEmailNotifications());
    }

    public boolean accountActive(User user) {
        return user != null && (user.getStatus() == null || AccountStatus.ACTIVE.name().equals(user.getStatus()));
    }

    public boolean hasUsableEmail(User user) {
        return user != null && user.getEmail() != null && user.getEmail().contains("@")
                && !user.getEmail().isBlank();
    }
}
