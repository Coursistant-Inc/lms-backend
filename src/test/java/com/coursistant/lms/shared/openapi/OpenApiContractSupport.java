package com.coursistant.lms.shared.openapi;

import com.coursistant.lms.shared.config.OpenApiConfig;
import com.coursistant.lms.shared.security.AuthPublicPaths;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared helpers for OpenAPI export, parse, inventory, security, naming, and drift checks.
 */
public final class OpenApiContractSupport {

    public static final Map<String, Integer> EXPECTED_COUNTS = Map.of(
            "auth", 21,
            "user", 13,
            "course", 79,
            "assignment", 39,
            "quiz", 29
    );

    public static final Map<String, String> MODULE_PACKAGES = Map.of(
            "auth", "com.coursistant.lms.module.auth",
            "user", "com.coursistant.lms.module.user",
            "course", "com.coursistant.lms.module.course",
            "assignment", "com.coursistant.lms.module.assignment",
            "quiz", "com.coursistant.lms.module.quiz"
    );

    private static final Pattern AUTO_DISAMBIG = Pattern.compile(".*_\\d+$");

    private OpenApiContractSupport() {
    }

    public static OpenAPI parseYaml(String yaml) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        List<String> messages = result.getMessages() == null ? List.of() : result.getMessages();
        if (result.getOpenAPI() == null) {
            throw new IllegalStateException("OpenAPI parse failed: " + messages);
        }
        List<String> errors = messages.stream()
                .filter(m -> m != null && !m.isBlank())
                .filter(m -> !m.toLowerCase(Locale.ROOT).contains("warning"))
                .toList();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("OpenAPI parse messages: " + errors);
        }
        return result.getOpenAPI();
    }

    public static void writeYaml(Path file, String yaml) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, yaml, StandardCharsets.UTF_8);
    }

    public static String normalizeYaml(String yaml) {
        Yaml parser = new Yaml();
        Object loaded = parser.load(yaml);
        Object sorted = sortRecursively(loaded);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options).dump(sorted);
    }

    @SuppressWarnings("unchecked")
    private static Object sortRecursively(Object node) {
        if (node instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sorted.put(String.valueOf(e.getKey()), sortRecursively(e.getValue()));
            }
            return sorted;
        }
        if (node instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(sortRecursively(item));
            }
            return out;
        }
        return node;
    }

    public static Set<String> operationKeys(OpenAPI openAPI) {
        Set<String> keys = new LinkedHashSet<>();
        if (openAPI.getPaths() == null) {
            return keys;
        }
        openAPI.getPaths().forEach((path, item) -> {
            for (PathItem.HttpMethod method : PathItem.HttpMethod.values()) {
                Operation op = item.readOperationsMap().get(method);
                if (op != null) {
                    keys.add(method.name() + " " + path);
                }
            }
        });
        return keys;
    }

    public static Set<String> operationIds(OpenAPI openAPI) {
        Set<String> ids = new LinkedHashSet<>();
        if (openAPI.getPaths() == null) {
            return ids;
        }
        openAPI.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            if (op.getOperationId() != null) {
                ids.add(op.getOperationId());
            }
        }));
        return ids;
    }

    public static List<String> collectMissingOperationIds(OpenAPI openAPI) {
        List<String> missing = new ArrayList<>();
        if (openAPI.getPaths() == null) {
            return missing;
        }
        openAPI.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            if (op.getOperationId() == null || op.getOperationId().isBlank()) {
                missing.add(method.name() + " " + path);
            }
        }));
        return missing;
    }

    public static List<String> collectAutoDisambiguatedSchemas(OpenAPI openAPI) {
        List<String> bad = new ArrayList<>();
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return bad;
        }
        for (String name : openAPI.getComponents().getSchemas().keySet()) {
            if (AUTO_DISAMBIG.matcher(name).matches()) {
                bad.add(name);
            }
        }
        return bad;
    }

    public static List<String> collectUnresolvedRefs(OpenAPI openAPI) {
        // swagger-parser resolves refs; residual check on schema map keys referenced elsewhere is best-effort
        List<String> issues = new ArrayList<>();
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return issues;
        }
        Set<String> schemaNames = openAPI.getComponents().getSchemas().keySet();
        for (Schema<?> schema : openAPI.getComponents().getSchemas().values()) {
            collectRefIssues(schema, schemaNames, issues);
        }
        return issues;
    }

    @SuppressWarnings("rawtypes")
    private static void collectRefIssues(Schema schema, Set<String> schemaNames, List<String> issues) {
        if (schema == null) {
            return;
        }
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String name = ref.substring(ref.lastIndexOf('/') + 1);
            if (!schemaNames.contains(name)) {
                issues.add("Unresolved $ref: " + ref);
            }
        }
        if (schema.getProperties() != null) {
            for (Object prop : schema.getProperties().values()) {
                collectRefIssues((Schema) prop, schemaNames, issues);
            }
        }
        if (schema.getItems() != null) {
            collectRefIssues(schema.getItems(), schemaNames, issues);
        }
        if (schema.getAllOf() != null) {
            for (Object s : schema.getAllOf()) {
                collectRefIssues((Schema) s, schemaNames, issues);
            }
        }
        if (schema.getOneOf() != null) {
            for (Object s : schema.getOneOf()) {
                collectRefIssues((Schema) s, schemaNames, issues);
            }
        }
    }

    public static List<String> collectSecurityMismatches(OpenAPI openAPI) {
        Set<String> publicKeys = AuthPublicPaths.publicMethodPaths();
        List<String> mismatches = new ArrayList<>();
        if (openAPI.getPaths() == null) {
            return mismatches;
        }
        openAPI.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
            String key = method.name() + " " + path;
            boolean expectPublic = publicKeys.contains(key);
            boolean isPublic = isExplicitlyPublic(op);
            boolean hasBearer = hasBearer(op, openAPI);
            if (expectPublic && !isPublic) {
                mismatches.add("Expected public (security: []) but not: " + key);
            }
            if (!expectPublic && isPublic) {
                mismatches.add("Unexpected public security on protected op: " + key);
            }
            if (!expectPublic && !hasBearer && !isPublic) {
                // inherits root security — OK if root has bearer
                boolean rootHasBearer = openAPI.getSecurity() != null && openAPI.getSecurity().stream()
                        .anyMatch(req -> req.containsKey(OpenApiConfig.BEARER_SCHEME));
                if (!rootHasBearer) {
                    mismatches.add("Protected op missing bearerAuth: " + key);
                }
            }
        }));
        return mismatches;
    }

    private static boolean isExplicitlyPublic(Operation op) {
        return op.getSecurity() != null && op.getSecurity().isEmpty();
    }

    private static boolean hasBearer(Operation op, OpenAPI openAPI) {
        List<SecurityRequirement> reqs = op.getSecurity();
        if (reqs == null) {
            reqs = openAPI.getSecurity();
        }
        if (reqs == null) {
            return false;
        }
        return reqs.stream().anyMatch(r -> r.containsKey(OpenApiConfig.BEARER_SCHEME));
    }

    public static Set<String> controllerInventory(RequestMappingHandlerMapping mapping, String modulePackage) {
        Set<String> keys = new LinkedHashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            Class<?> beanType = handler.getBeanType();
            if (!beanType.getName().startsWith(modulePackage + ".")
                    && !beanType.getName().equals(modulePackage)) {
                continue;
            }
            if (!beanType.getName().contains(".controller.")) {
                continue;
            }
            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = info.getPatternValues();
            if (patterns.isEmpty() && info.getPathPatternsCondition() != null) {
                patterns = info.getPathPatternsCondition().getPatternValues();
            }
            Set<org.springframework.web.bind.annotation.RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                continue;
            }
            for (String pattern : patterns) {
                String path = pattern.startsWith("/") ? pattern : "/" + pattern;
                for (org.springframework.web.bind.annotation.RequestMethod method : methods) {
                    keys.add(method.name() + " " + path);
                }
            }
        }
        return keys;
    }

    public static void assertInventoryMatch(String module, Set<String> fromControllers, Set<String> fromOpenApi) {
        Set<String> missing = new LinkedHashSet<>(fromControllers);
        missing.removeAll(fromOpenApi);
        Set<String> extra = new LinkedHashSet<>(fromOpenApi);
        extra.removeAll(fromControllers);
        Integer expected = EXPECTED_COUNTS.get(module);
        if (expected != null && fromControllers.size() != expected) {
            throw new AssertionError(module + " controller inventory size " + fromControllers.size()
                    + " != expected " + expected + ": " + fromControllers);
        }
        if (!missing.isEmpty() || !extra.isEmpty()) {
            throw new AssertionError(module + " inventory mismatch. missing=" + missing + " extra=" + extra);
        }
    }

    public static void assertNoDuplicateOperationIds(List<OpenAPI> specs) {
        Map<String, Long> counts = specs.stream()
                .flatMap(api -> operationIds(api).stream())
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        List<String> dupes = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!dupes.isEmpty()) {
            throw new AssertionError("Duplicate operationIds across modules: " + dupes);
        }
    }

    public static void assertNoCrossModuleDuplicates(Map<String, Set<String>> perModule) {
        Map<String, List<String>> owners = new LinkedHashMap<>();
        perModule.forEach((module, keys) -> {
            for (String key : keys) {
                owners.computeIfAbsent(key, k -> new ArrayList<>()).add(module);
            }
        });
        List<String> dupes = owners.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> e.getKey() + " -> " + e.getValue())
                .toList();
        if (!dupes.isEmpty()) {
            throw new AssertionError("Cross-module path duplicates: " + dupes);
        }
    }

    public static void assertDrift(String expectedYaml, String actualYaml) {
        String a = normalizeYaml(expectedYaml);
        String b = normalizeYaml(actualYaml);
        if (!Objects.equals(a, b)) {
            throw new AssertionError("OpenAPI drift detected (normalized YAML differs)");
        }
    }
}
