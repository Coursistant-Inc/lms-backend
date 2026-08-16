package com.coursistant.lms.shared.config;

import com.coursistant.lms.shared.security.AuthPublicPaths;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";
    public static final String API_VERSION = "1.0.0";

    public static final List<String> MODULES = List.of("auth", "user", "course", "assignment", "quiz");

    @Bean
    public OpenAPI lmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS API")
                        .version(API_VERSION)
                        .description("Coursistant LMS REST API contract (OpenAPI 3)."))
                .servers(List.of(new Server().url("/api").description("Servlet context-path")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from POST /v1/auth/login (Authorization: Bearer <token>)."))
                        .addSchemas("ApiErrorResponse", apiErrorSchema())
                        .addSchemas("ApiSuccessEnvelope", apiSuccessEnvelopeSchema()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * Clears security on AuthPublicPaths exact method+path pairs for every group.
     */
    @Bean
    public GlobalOpenApiCustomizer publicPathSecurityCustomizer() {
        return OpenApiConfig::applyPublicPathSecurity;
    }

    static void applyPublicPathSecurity(OpenAPI openApi) {
        Set<String> publicKeys = AuthPublicPaths.publicMethodPaths();
        if (openApi.getPaths() == null) {
            return;
        }
        for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem item = entry.getValue();
            clearSecurityIfPublic(item.getGet(), HttpMethod.GET, path, publicKeys);
            clearSecurityIfPublic(item.getPost(), HttpMethod.POST, path, publicKeys);
            clearSecurityIfPublic(item.getPut(), HttpMethod.PUT, path, publicKeys);
            clearSecurityIfPublic(item.getPatch(), HttpMethod.PATCH, path, publicKeys);
            clearSecurityIfPublic(item.getDelete(), HttpMethod.DELETE, path, publicKeys);
        }
    }

    private static void clearSecurityIfPublic(Operation operation, HttpMethod method, String path,
                                             Set<String> publicKeys) {
        if (operation == null) {
            return;
        }
        if (publicKeys.contains(method.name() + " " + path)) {
            operation.setSecurity(Collections.emptyList());
        }
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .packagesToScan("com.coursistant.lms.module.auth")
                .pathsToMatch("/v1", "/v1/auth/**", "/v2/admins/**",
                        "/v2/tenant/managed-users/**", "/v2/system/managed-users/**")
                .addOpenApiCustomizer(OpenApiConfig::applyPublicPathSecurity)
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .packagesToScan("com.coursistant.lms.module.user")
                .pathsToMatch("/v2/users/**", "/v2/me/profile/**", "/v2/admin/users/**")
                .addOpenApiCustomizer(OpenApiConfig::applyPublicPathSecurity)
                .build();
    }

    @Bean
    public GroupedOpenApi courseApi() {
        return GroupedOpenApi.builder()
                .group("course")
                .packagesToScan("com.coursistant.lms.module.course")
                .pathsToMatch("/v2/courses/**", "/v2/me/courses/**", "/v2/me/events/**",
                        "/v2/me/announcements/**", "/v2/me/teaching/**", "/v2/admin/courses/**")
                .addOpenApiCustomizer(OpenApiConfig::applyPublicPathSecurity)
                .build();
    }

    @Bean
    public GroupedOpenApi assignmentApi() {
        return GroupedOpenApi.builder()
                .group("assignment")
                .packagesToScan("com.coursistant.lms.module.assignment")
                .pathsToMatch("/v2/courses/*/assignments/**", "/v2/courses/*/my-grades/**",
                        "/v2/me/assignments/**", "/v2/system/grade-corrections/**")
                .addOpenApiCustomizer(OpenApiConfig::applyPublicPathSecurity)
                .build();
    }

    @Bean
    public GroupedOpenApi quizApi() {
        return GroupedOpenApi.builder()
                .group("quiz")
                .packagesToScan("com.coursistant.lms.module.quiz")
                .pathsToMatch("/v2/courses/*/quizzes/**")
                .addOpenApiCustomizer(OpenApiConfig::applyPublicPathSecurity)
                .build();
    }

    @SuppressWarnings("rawtypes")
    private static Schema apiErrorSchema() {
        return new ObjectSchema()
                .description("Error envelope rendered by ApiExceptionHandler (data optional).")
                .addProperty("status", new IntegerSchema().example(404))
                .addProperty("code", new StringSchema()
                        .description("Stable ErrorType.name()")
                        .example("USER_NOT_FOUND"))
                .addProperty("data", new ObjectSchema().nullable(true))
                .addProperty("message", new StringSchema().example("User Does Not Exist"))
                .addProperty("timestamp", new StringSchema()
                        .format("date-time")
                        .example("2026-07-23T10:00:00Z"))
                .required(List.of("status", "code", "message", "timestamp"));
    }

    @SuppressWarnings("rawtypes")
    private static Schema apiSuccessEnvelopeSchema() {
        return new ObjectSchema()
                .description("Success envelope shape (data type varies per operation).")
                .addProperty("status", new IntegerSchema().example(200))
                .addProperty("code", new StringSchema().example("SUCCESS"))
                .addProperty("data", new ObjectSchema().nullable(true))
                .addProperty("message", new StringSchema().example("Success"))
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .required(List.of("status", "code", "message", "timestamp"));
    }

    public static String normalizeMethod(String method) {
        return method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
    }
}
