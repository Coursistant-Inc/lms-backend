package com.coursistant.lms.module.interaction.notification.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationJson {

    private static final TypeReference<LinkedHashMap<String, String>> MAP =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public NotificationJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeVars(Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(vars);
            if (json.length() > 2000) {
                return json.substring(0, 2000);
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification template vars", e);
        }
    }

    public Map<String, String> readVars(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            LinkedHashMap<String, String> map = objectMapper.readValue(json, MAP);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
