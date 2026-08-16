package com.coursistant.lms.shared.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerStorageFailureTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StorageFailureProbeController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void storageFailure_is503WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/__test/storage-failure").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("STORAGE_FAILURE"))
                .andExpect(jsonPath("$.message").value("Failed to load preview"))
                .andExpect(content().string(not(containsString("software.amazon"))))
                .andExpect(content().string(not(containsString("S3Exception"))))
                .andExpect(content().string(not(containsString("S3StorageException"))))
                .andExpect(content().string(not(containsString("stacktrace"))))
                .andExpect(content().string(not(containsString("Foo.java"))));
    }

    @RestController
    static class StorageFailureProbeController {
        @GetMapping("/__test/storage-failure")
        public void fail() {
            ApiException ex = new ApiException(ErrorType.STORAGE_FAILURE, "Failed to load preview");
            ex.initCause(new RuntimeException(
                    "software.amazon.awssdk.services.s3.model.S3Exception: Access Denied\n"
                            + "\tat com.amazonaws.Foo.bar(Foo.java:1)"));
            throw ex;
        }
    }
}
