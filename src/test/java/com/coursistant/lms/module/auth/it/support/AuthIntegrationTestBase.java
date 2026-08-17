package com.coursistant.lms.module.auth.it.support;

import com.coursistant.lms.SpringbootApplication;
import com.coursistant.lms.module.auth.support.FakeEmailCapture;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.Set;

/**
 * Shared Auth IT base: MySQL 8.0.36 + Redis 7.2 via Testcontainers when Docker is available.
 * Emergency local isolated DB only when -Dauth.it.allow-local=true (never lms_v2).
 */
@SpringBootTest(classes = SpringbootApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("auth-it")
@Import(AuthItTestConfiguration.class)
public abstract class AuthIntegrationTestBase {

    public static final String MYSQL_IMAGE = "mysql:8.0.36";
    public static final String REDIS_IMAGE = "redis:7.2-alpine";

    private static final boolean DOCKER = detectDocker();
    private static final boolean ALLOW_LOCAL = Boolean.getBoolean("auth.it.allow-local");

    private static boolean detectDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = DOCKER
            ? new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
            .withDatabaseName("lms_auth_it")
            .withUsername("auth_it")
            .withPassword("auth_it")
            .withReuse(true)
            : null;

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = DOCKER
            ? new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
            .withReuse(true)
            : null;

    static {
        if (DOCKER) {
            MYSQL.start();
            REDIS.start();
        } else if (!ALLOW_LOCAL) {
            throw new IllegalStateException(
                    "Auth IT requires Docker Testcontainers (" + MYSQL_IMAGE + ", " + REDIS_IMAGE
                            + "). Start Docker, or pass -Dauth.it.allow-local=true with isolated lms_auth_it DB.");
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        if (DOCKER) {
            registry.add("spring.datasource.url", () -> {
                String base = MYSQL.getJdbcUrl();
                String sep = base.contains("?") ? "&" : "?";
                return base + sep + "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
                        + "&allowPublicKeyRetrieval=true&connectTimeout=2000&socketTimeout=3000";
            });
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
            registry.add("REDIS_DEFAULT_PASSWORD", () -> "");
        } else {
            // Isolated local schema — not developer lms_v2
            String url = System.getProperty("auth.it.jdbc-url",
                    "jdbc:mysql://localhost:3306/lms_auth_it?useUnicode=true&characterEncoding=utf-8"
                            + "&allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true"
                            + "&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username",
                    () -> System.getProperty("auth.it.jdbc-user", System.getenv().getOrDefault("DB_USERNAME", "root")));
            registry.add("spring.datasource.password",
                    () -> System.getProperty("auth.it.jdbc-password", System.getenv().getOrDefault("DB_PASSWORD", "")));
            registry.add("spring.data.redis.host", () -> System.getProperty("auth.it.redis-host", "localhost"));
            registry.add("spring.data.redis.port", () -> System.getProperty("auth.it.redis-port", "6379"));
            String redisPw = System.getProperty("auth.it.redis-password",
                    System.getenv().getOrDefault("REDIS_DEFAULT_PASSWORD", ""));
            registry.add("REDIS_DEFAULT_PASSWORD", () -> redisPw);
        }
        // Prevent accidental use of production key paths from application.yml
        registry.add("token.private-key-path", () -> "classpath:test-private.pem");
        registry.add("token.public-key-path", () -> "classpath:test-public.pem");
        registry.add("MAIL_USERNAME", () -> "test@example.com");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("API_BASE_URL", () -> "http://localhost");
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected DataSource dataSource;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected AuthTestDataFactory dataFactory;
    @Autowired
    protected FakeEmailCapture fakeEmailCapture;
    @Autowired
    protected AuthItTestConfiguration.MutableClock mutableClock;
    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    private static boolean schemaApplied;

    @BeforeEach
    void resetAuthState() {
        ensureSchema();
        mutableClock.setInstant(AuthItTestConfiguration.FIXED_INSTANT);
        fakeEmailCapture.clear();
        wipeAuthTables();
        wipeAuthRedisKeys();
    }

    private void ensureSchema() {
        if (schemaApplied) {
            return;
        }
        synchronized (AuthIntegrationTestBase.class) {
            if (schemaApplied) {
                return;
            }
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("auth-it-schema.sql"));
            populator.setContinueOnError(false);
            populator.execute(dataSource);
            schemaApplied = true;
        }
    }

    /** Subclasses that add FK-dependent tables should wipe those first, then call super. */
    protected void wipeAuthTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM identity_audit");
        jdbcTemplate.update("DELETE FROM email_outbox");
        jdbcTemplate.update("DELETE FROM account_identity");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.update("DELETE FROM admin");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    protected void wipeAuthRedisKeys() {
        try {
            Set<String> keys = stringRedisTemplate.keys("auth:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            keys = stringRedisTemplate.keys("email:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            keys = stringRedisTemplate.keys("refresh:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            keys = stringRedisTemplate.keys("ratelimit:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
            // RedisTemplate on db0 may not see refresh db6 keys; RefreshTokenService cleans its own.
        }
    }
}
