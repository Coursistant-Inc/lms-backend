package com.coursistant.lms.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigPublicSecurityTest {

    @Test
    void applyPublicPathSecurity_matchesPublicMethodPathsExactly() {
        Operation avatarGet = new Operation()
                .addSecurityItem(new SecurityRequirement().addList(OpenApiConfig.BEARER_SCHEME));
        Operation nearbyGet = new Operation();
        Operation healthGet = new Operation()
                .addSecurityItem(new SecurityRequirement().addList(OpenApiConfig.BEARER_SCHEME));
        Operation avatarPut = new Operation()
                .addSecurityItem(new SecurityRequirement().addList(OpenApiConfig.BEARER_SCHEME));

        OpenAPI api = new OpenAPI().paths(new Paths()
                .addPathItem("/v2/users/{userId}/avatar", new PathItem().get(avatarGet).put(avatarPut))
                .addPathItem("/v2/users/{id}", new PathItem().get(nearbyGet))
                .addPathItem("/v1", new PathItem().get(healthGet)));

        OpenApiConfig.applyPublicPathSecurity(api);

        assertTrue(api.getPaths().get("/v2/users/{userId}/avatar").getGet().getSecurity().isEmpty());
        assertNull(api.getPaths().get("/v2/users/{id}").getGet().getSecurity());
        assertTrue(api.getPaths().get("/v1").getGet().getSecurity().isEmpty());
        assertTrue(api.getPaths().get("/v2/users/{userId}/avatar").getPut().getSecurity()
                .stream()
                .anyMatch(req -> req.containsKey(OpenApiConfig.BEARER_SCHEME)));
    }
}
