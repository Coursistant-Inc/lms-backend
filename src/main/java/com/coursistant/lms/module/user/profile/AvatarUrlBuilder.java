package com.coursistant.lms.module.user.profile;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Builds public avatar proxy URLs from the current request context.
 * Never uses API_BASE_URL (may lack port).
 */
@Component
public class AvatarUrlBuilder {

    public String build(Integer userId, String avatarObjectKey) {
        return buildStatic(userId, avatarObjectKey);
    }

    public static String buildStatic(Integer userId, String avatarObjectKey) {
        if (userId == null || avatarObjectKey == null || avatarObjectKey.isBlank()) {
            return null;
        }
        String v = versionFromKey(avatarObjectKey);
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/v2/users/{userId}/avatar")
                    .queryParam("v", v)
                    .buildAndExpand(userId)
                    .toUriString();
        } catch (IllegalStateException ex) {
            // No request bound (e.g. background job): relative URL with known context-path.
            return "/api/v2/users/" + userId + "/avatar?v=" + v;
        }
    }

    static String versionFromKey(String avatarObjectKey) {
        // key: {userId}/{uuid}.jpg|png → use uuid (filename without extension)
        String name = avatarObjectKey;
        int slash = name.lastIndexOf('/');
        if (slash >= 0 && slash < name.length() - 1) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.isBlank() ? Integer.toHexString(avatarObjectKey.hashCode()) : name;
    }
}
