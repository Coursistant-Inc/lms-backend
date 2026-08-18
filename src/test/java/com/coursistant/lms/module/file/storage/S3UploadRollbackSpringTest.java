package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.module.course.storage.entity.MinioObjectOutbox;
import com.coursistant.lms.module.course.storage.repository.MinioObjectOutboxMapper;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = S3UploadRollbackSpringTest.Config.class)
class S3UploadRollbackSpringTest {

    @Autowired
    private TxRunner txRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void schema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS minio_object_outbox (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  upload_operation_id VARCHAR(36) NULL,
                  bucket VARCHAR(128) NOT NULL,
                  object_key VARCHAR(512) NOT NULL,
                  action VARCHAR(32) NOT NULL,
                  status VARCHAR(16) NOT NULL,
                  retry_count INT NOT NULL DEFAULT 0,
                  next_retry_at TIMESTAMP NOT NULL,
                  lease_until TIMESTAMP NULL,
                  last_error VARCHAR(512) NULL,
                  course_id INT NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("DELETE FROM minio_object_outbox");
    }

    @Test
    void orphanV5_springProxyRollbackKeepsIndependentAbortRow() {
        assertThrows(IllegalStateException.class, () -> txRunner.rememberThenFail());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM minio_object_outbox", Integer.class);
        assertEquals(1, count);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT bucket, object_key, action, status, course_id FROM minio_object_outbox WHERE object_key = ?",
                "tx-fail.pdf");
        assertEquals("lms-uploads", col(row, "bucket"));
        assertEquals("tx-fail.pdf", col(row, "object_key"));
        assertEquals(MinioObjectOutbox.ACTION_ABORT_STAGING, col(row, "action"));
        assertEquals(MinioObjectOutbox.STATUS_PENDING, col(row, "status"));
        assertEquals(9, ((Number) col(row, "course_id")).intValue());
    }

    @Test
    void orphanV5_springProxyCommitDoesNotInsertAbortRow() {
        txRunner.rememberThenCommit();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM minio_object_outbox WHERE object_key = ?", Integer.class, "tx-ok.pdf");
        assertEquals(0, count);
    }

    private static Object col(Map<String, Object> row, String name) {
        if (row.containsKey(name)) {
            return row.get(name);
        }
        return row.get(name.toUpperCase());
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:s3UploadRollback;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources("classpath:mapper/course/MinioObjectOutboxMapper.xml"));
            return factory.getObject();
        }

        @Bean
        MapperFactoryBean<MinioObjectOutboxMapper> minioObjectOutboxMapper(SqlSessionFactory sqlSessionFactory) {
            MapperFactoryBean<MinioObjectOutboxMapper> factory =
                    new MapperFactoryBean<>(MinioObjectOutboxMapper.class);
            factory.setSqlSessionFactory(sqlSessionFactory);
            return factory;
        }

        @Bean
        S3ObjectStorage s3ObjectStorage() {
            return mock(S3ObjectStorage.class);
        }

        @Bean
        S3ObjectKeyResolver s3ObjectKeyResolver() {
            return mock(S3ObjectKeyResolver.class);
        }

        @Bean
        MinioOutboxService minioOutboxService() {
            return new MinioOutboxService();
        }

        @Bean
        S3UploadRollback s3UploadRollback(MinioOutboxService minioOutboxService) {
            return new S3UploadRollback(minioOutboxService);
        }

        @Bean
        TxRunner txRunner(S3UploadRollback s3UploadRollback) {
            return new TxRunner(s3UploadRollback);
        }
    }

    static class TxRunner {
        private final S3UploadRollback s3UploadRollback;

        TxRunner(S3UploadRollback s3UploadRollback) {
            this.s3UploadRollback = s3UploadRollback;
        }

        @Transactional
        public void rememberThenFail() {
            S3UploadRollback.Scope scope = s3UploadRollback.open(9, null);
            scope.remember("lms-uploads", "tx-fail.pdf");
            throw new IllegalStateException("forced rollback");
        }

        @Transactional
        public void rememberThenCommit() {
            s3UploadRollback.open(9, null).remember("lms-uploads", "tx-ok.pdf");
        }
    }
}
