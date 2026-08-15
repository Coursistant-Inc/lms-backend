package com.coursistant.lms.module.auth.session.dto;

import com.coursistant.lms.module.user.account.entity.Account;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthSessionDtoOpenApiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void loginRequest_deserializesAndMapsToAccountFields() throws Exception {
        String json = "{\"email\":\"student@example.com\",\"password\":\"Passw0rd1\",\"role\":\"USER\"}";
        LoginRequest request = mapper.readValue(json, LoginRequest.class);

        assertEquals("student@example.com", request.getEmail());
        assertEquals("Passw0rd1", request.getPassword());
        assertEquals("USER", request.getRole());

        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPassword(request.getPassword());
        account.setRole(request.getRole());

        assertEquals(request.getEmail(), account.getEmail());
        assertEquals(request.getPassword(), account.getPassword());
        assertEquals(request.getRole(), account.getRole());
    }

    @Test
    void authResult_jsonOmitsRefreshToken_evenWhenSet() throws Exception {
        AuthResult result = new AuthResult();
        result.setUserId(21);
        result.setEmail("student@example.com");
        result.setRole("USER");
        result.setAccessToken("access-abc");
        result.setRefreshToken("refresh-secret-xyz");

        String json = mapper.writeValueAsString(result);
        JsonNode node = mapper.readTree(json);

        assertFalse(json.contains("refresh-secret-xyz"));
        assertFalse(node.has("refreshToken"));
        assertEquals("access-abc", node.get("accessToken").asText());
        assertEquals("student@example.com", node.get("email").asText());
    }
}
