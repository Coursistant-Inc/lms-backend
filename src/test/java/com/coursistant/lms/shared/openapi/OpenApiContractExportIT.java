package com.coursistant.lms.shared.openapi;

import com.coursistant.lms.shared.config.OpenApiConfig;
import com.coursistant.lms.shared.security.AuthPublicPaths;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the app on a random port under the openapi profile and exports grouped OpenAPI YAML.
 *
 * <p>System properties:
 * <ul>
 *   <li>{@code openapi.modules} — comma-separated module list or {@code all} (default all)</li>
 *   <li>{@code openapi.outputDir} — output directory (default {@code target/openapi-bootstrap})</li>
 *   <li>{@code openapi.verifyCommitted} — when true, also drift-check against {@code docs/api}</li>
 *   <li>{@code openapi.runNegativeChecks} — when true, assert negative fixtures fail</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("openapi")
@Import(OpenApiTestInfrastructureConfig.class)
class OpenApiContractExportIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Value("${openapi.modules:all}")
    private String modulesProp;

    @Value("${openapi.outputDir:target/openapi-bootstrap}")
    private String outputDir;

    @Value("${openapi.verifyCommitted:false}")
    private boolean verifyCommitted;

    @Value("${openapi.runNegativeChecks:true}")
    private boolean runNegativeChecks;

    @Test
    void exportAndValidateGroupedContracts() throws Exception {
        List<String> modules = resolveModules(modulesProp);
        Path out = Path.of(outputDir);
        Files.createDirectories(out);

        Map<String, Set<String>> perModuleOps = new LinkedHashMap<>();
        List<OpenAPI> parsed = new ArrayList<>();

        for (String module : modules) {
            String yaml = fetchGroupYaml(module);
            assertFalse(yaml.isBlank(), "Empty OpenAPI YAML for group " + module);
            Path file = out.resolve(module + ".openapi.yaml");
            OpenApiContractSupport.writeYaml(file, yaml);

            OpenAPI api = OpenApiContractSupport.parseYaml(yaml);
            parsed.add(api);

            Set<String> controllerKeys = OpenApiContractSupport.controllerInventory(
                    requestMappingHandlerMapping, OpenApiContractSupport.MODULE_PACKAGES.get(module));
            Set<String> openApiKeys = OpenApiContractSupport.operationKeys(api);
            OpenApiContractSupport.assertInventoryMatch(module, controllerKeys, openApiKeys);
            perModuleOps.put(module, openApiKeys);

            List<String> missingIds = OpenApiContractSupport.collectMissingOperationIds(api);
            assertTrue(missingIds.isEmpty(), module + " missing operationId: " + missingIds);

            List<String> badSchemas = OpenApiContractSupport.collectAutoDisambiguatedSchemas(api);
            assertTrue(badSchemas.isEmpty(), module + " auto-disambiguated schemas: " + badSchemas);

            List<String> sec = OpenApiContractSupport.collectSecurityMismatches(api);
            assertTrue(sec.isEmpty(), module + " security mismatches: " + sec);

            if (verifyCommitted) {
                Path committed = Path.of("docs/api", module + ".openapi.yaml");
                assertTrue(Files.exists(committed), "Missing committed contract: " + committed);
                String committedYaml = Files.readString(committed);
                OpenApiContractSupport.assertDrift(committedYaml, yaml);
            }
        }

        if (modules.containsAll(OpenApiConfig.MODULES)) {
            OpenApiContractSupport.assertNoCrossModuleDuplicates(perModuleOps);
            OpenApiContractSupport.assertNoDuplicateOperationIds(parsed);
        }

        // AuthPublicPaths sync smoke: public set non-empty and contains login
        assertTrue(AuthPublicPaths.publicMethodPaths().contains("POST /v1/auth/login"));
        assertEquals(8, AuthPublicPaths.publicMethodPaths().size());

        if (runNegativeChecks) {
            runNegativeFixtures();
        }
    }

    private String fetchGroupYaml(String module) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/v3/api-docs.yaml/" + module, String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Failed to fetch OpenAPI for " + module + ": " + response.getStatusCode());
        return response.getBody() == null ? "" : response.getBody();
    }

    private static List<String> resolveModules(String prop) {
        if (prop == null || prop.isBlank() || "all".equalsIgnoreCase(prop.trim())) {
            return OpenApiConfig.MODULES;
        }
        List<String> list = new ArrayList<>();
        for (String part : prop.split(",")) {
            String m = part.trim().toLowerCase();
            if (!m.isEmpty()) {
                list.add(m);
            }
        }
        return list;
    }

    private void runNegativeFixtures() {
        // Intentionally broken YAML must fail parse
        boolean failed = false;
        try {
            OpenApiContractSupport.parseYaml("openapi: 3.0.1\ninfo:\n  title: x\npaths: {");
        } catch (IllegalStateException expected) {
            failed = true;
        }
        assertTrue(failed, "Broken YAML should fail parser");

        // Fake auto-disambiguated schema name detection
        OpenAPI synthetic = OpenApiContractSupport.parseYaml("""
                openapi: 3.0.1
                info:
                  title: t
                  version: "1"
                paths: {}
                components:
                  schemas:
                    CreateRequest_1:
                      type: object
                """);
        assertFalse(OpenApiContractSupport.collectAutoDisambiguatedSchemas(synthetic).isEmpty());
    }
}
