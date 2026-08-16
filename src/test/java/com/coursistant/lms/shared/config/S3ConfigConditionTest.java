package com.coursistant.lms.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class S3ConfigConditionTest {

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class))
                .withUserConfiguration(S3Properties.class);
    }

    @Test
    void missingEnabledFlag_withRegionAndBucket_matchesDefaultOnCondition() {
        baseRunner()
                .withUserConfiguration(EnabledMarkerConfig.class)
                .withPropertyValues("aws.s3.region=us-west-2", "aws.s3.bucket=lms-test-bucket")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(S3Client.class);
                    assertThat(context.getBean(S3Properties.class).isEnabled()).isTrue();
                });
    }

    @Test
    void enabledTrue_blankBucket_failsFast() {
        baseRunner()
                .withPropertyValues("aws.s3.enabled=true", "aws.s3.region=us-west-2", "aws.s3.bucket=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void enabledFalse_failsFast() {
        baseRunner()
                .withUserConfiguration(EnabledMarkerConfig.class)
                .withPropertyValues("aws.s3.enabled=false", "aws.s3.region=us-west-2", "aws.s3.bucket=lms-test-bucket")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("aws.s3.enabled=false is not allowed; S3 object storage is required");
                });
    }

    @Configuration
    @ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class EnabledMarkerConfig {
        @Bean
        S3Client s3Client() {
            return mock(S3Client.class);
        }
    }
}
