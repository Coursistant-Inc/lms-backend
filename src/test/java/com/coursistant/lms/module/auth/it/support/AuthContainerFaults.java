package com.coursistant.lms.module.auth.it.support;

/**
 * Pause/unpause Testcontainers without changing mapped ports.
 * Primary fault injection for Auth Phase 3 (no stop/start).
 */
public final class AuthContainerFaults {

    private AuthContainerFaults() {
    }

    public static void pauseRedis() {
        requireDocker();
        AuthIntegrationTestBase.REDIS.getDockerClient()
                .pauseContainerCmd(AuthIntegrationTestBase.REDIS.getContainerId())
                .exec();
    }

    public static void unpauseRedis() {
        requireDocker();
        try {
            AuthIntegrationTestBase.REDIS.getDockerClient()
                    .unpauseContainerCmd(AuthIntegrationTestBase.REDIS.getContainerId())
                    .exec();
        } catch (Exception ignored) {
            // already running
        }
    }

    public static void pauseMysql() {
        requireDocker();
        AuthIntegrationTestBase.MYSQL.getDockerClient()
                .pauseContainerCmd(AuthIntegrationTestBase.MYSQL.getContainerId())
                .exec();
    }

    public static void unpauseMysql() {
        requireDocker();
        try {
            AuthIntegrationTestBase.MYSQL.getDockerClient()
                    .unpauseContainerCmd(AuthIntegrationTestBase.MYSQL.getContainerId())
                    .exec();
        } catch (Exception ignored) {
            // already running
        }
    }

    private static void requireDocker() {
        if (AuthIntegrationTestBase.REDIS == null || AuthIntegrationTestBase.MYSQL == null) {
            throw new IllegalStateException("Container fault injection requires Docker Testcontainers");
        }
    }
}
