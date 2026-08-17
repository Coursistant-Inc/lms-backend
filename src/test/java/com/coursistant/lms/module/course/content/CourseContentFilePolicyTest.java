package com.coursistant.lms.module.course.content;

import com.coursistant.lms.module.file.storage.FileSignatureSamples;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseContentFilePolicyTest {

    private final CourseContentFilePolicy policy = new CourseContentFilePolicy();

    @BeforeEach
    void sizeLimit() {
        ReflectionTestUtils.setField(policy, "maxFileBytes", 209_715_200L);
    }

    @Test
    void xssU1_htmlNamedPng_isUnsupported() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.png", "text/html", FileSignatureSamples.HTML);
        ApiException ex = assertThrows(ApiException.class, () -> policy.validateMaterialFile(file));
        assertEquals(ErrorType.UNSUPPORTED_FILE_TYPE, ex.getErrorType());
    }

    @Test
    void xssU2_realPng_returnsCanonicalPngMime() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.png", "text/html", FileSignatureSamples.PNG);
        assertEquals("image/png", policy.validateMaterialFile(file));
    }

    @Test
    void xssU3_jpegBytesNamedPng_isUnsupported() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.png", "image/png", FileSignatureSamples.JPEG);
        ApiException ex = assertThrows(ApiException.class, () -> policy.validateMaterialFile(file));
        assertEquals(ErrorType.UNSUPPORTED_FILE_TYPE, ex.getErrorType());
    }

    @Test
    void syllabusHtmlNamedPdf_isUnsupported() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "s.pdf", "application/pdf", FileSignatureSamples.HTML);
        ApiException ex = assertThrows(ApiException.class, () -> policy.validateSyllabusPdf(file));
        assertEquals(ErrorType.UNSUPPORTED_FILE_TYPE, ex.getErrorType());
    }
}
