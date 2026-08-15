package com.coursistant.lms.module.user.account.dto;

import com.coursistant.lms.module.user.account.controller.UserController;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.profile.dto.ProfileResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAdminResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void toResponse_omitsPasswordNewPasswordVerificationAuthVersion() throws Exception {
        User user = new User();
        user.setId(385);
        user.setTenantId(1);
        user.setUsername("alex");
        user.setName("Alex Rivera");
        user.setEmail("regtest1@example.com");
        user.setRole("USER");
        user.setLevel("STUDENT");
        user.setStatus("ACTIVE");
        user.setAvatar("avatars/385/abc.jpg");
        user.setMustChangePassword(false);
        user.setEmailNotifications(true);
        user.setPassword("$2a$hashed-secret");
        user.setNewPassword("plaintext-new");
        user.setVerification("123456");
        user.setAuthVersion(7);

        UserAdminResponse response = UserController.toResponse(user);
        String json = MAPPER.writeValueAsString(response);
        JsonNode node = MAPPER.readTree(json);

        assertFalse(node.has("password"), json);
        assertFalse(node.has("newPassword"), json);
        assertFalse(node.has("verification"), json);
        assertFalse(node.has("authVersion"), json);
        assertFalse(json.contains("$2a$hashed-secret"));
        assertFalse(json.contains("plaintext-new"));
        assertFalse(json.contains("123456"));

        assertEquals(385, response.getId());
        assertEquals(1, response.getTenantId());
        assertTrue(json.contains("regtest1@example.com"));
        assertTrue(node.has("mustChangePassword"));
        assertTrue(node.has("emailNotifications"));
        assertTrue(node.has("avatar"));

        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            assertFalse(name.equals("password")
                    || name.equals("newPassword")
                    || name.equals("verification")
                    || name.equals("authVersion"), name);
        }
    }

    @Test
    void profileResponse_jsonHasNoSensitiveAccountFields() throws Exception {
        ProfileResponse profile = new ProfileResponse();
        profile.setUserId(385);
        profile.setDisplayName("Alex Rivera");
        profile.setEmail("regtest1@example.com");
        profile.setRole("USER");
        profile.setLevel("STUDENT");
        profile.setAvatarUrl("https://example.com/api/v2/users/385/avatar?v=abc");
        profile.setEmailNotifications(true);

        JsonNode node = MAPPER.readTree(MAPPER.writeValueAsString(profile));
        assertFalse(node.has("password"));
        assertFalse(node.has("newPassword"));
        assertFalse(node.has("verification"));
        assertFalse(node.has("authVersion"));
        assertTrue(node.has("avatarUrl"));
        assertEquals(385, node.get("userId").asInt());
    }
}
