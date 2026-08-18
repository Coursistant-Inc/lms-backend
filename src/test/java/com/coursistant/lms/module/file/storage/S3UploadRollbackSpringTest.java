package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = S3UploadRollbackSpringTest.Config.class)
class S3UploadRollbackSpringTest {

    static final MinioOutboxService OUTBOX = mock(MinioOutboxService.class);

    @Autowired
    private TxRunner txRunner;

    @BeforeEach
    void resetOutbox() {
        reset(OUTBOX);
    }

    @Test
    void orphanV5_springProxyRollbackEnqueuesIndependentAbort() {
        assertThrows(IllegalStateException.class, () -> txRunner.rememberThenFail());
        verify(OUTBOX).enqueueAbortStagingIndependent("lms-uploads", "tx-fail.pdf", 9, null);
    }

    @Test
    void orphanV5_springProxyCommitDoesNotEnqueue() {
        txRunner.rememberThenCommit();
        verify(OUTBOX, never()).enqueueAbortStagingIndependent(
                "lms-uploads", "tx-ok.pdf", 9, null);
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        S3UploadRollback s3UploadRollback() {
            return new S3UploadRollback(OUTBOX);
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
