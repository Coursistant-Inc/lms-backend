package com.coursistant.lms.module.auth.admin;

import com.coursistant.lms.module.auth.admin.controller.AdminController;
import com.coursistant.lms.module.auth.admin.dto.AdminResponse;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminResponseTest {

    @Test
    void toResponse_omitsPasswordAndAuthVersion() throws Exception {
        Admin admin = new Admin();
        admin.setId(1);
        admin.setUsername("admin");
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setPassword("$2a$hashed");
        admin.setAuthVersion(3);
        admin.setInvitation("secret");
        admin.setRole("SYSTEM_ADMIN");
        admin.setStatus("ACTIVE");

        AdminResponse response = AdminController.toResponse(admin);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("$2a$"));
        assertFalse(json.contains("authVersion"));
        assertFalse(json.contains("invitation"));
        assertTrue(json.contains("admin@example.com"));
        assertEquals(1, response.getId());
    }
}
