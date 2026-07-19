package com.alef.api.admin.upload;

import com.alef.api.admin.upload.dto.UploadResponseDto;
import com.alef.api.admin.upload.error.FileTooLargeException;
import com.alef.api.admin.upload.error.MissingUploadCategoryException;
import com.alef.api.admin.upload.error.UnsupportedFileTypeException;
import com.alef.api.admin.upload.error.UploadExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for POST /api/admin/uploads (design.md §A.6, F12-AC20..AC22).
 * Security filters disabled -- see AdminProjectControllerTest's class doc for rationale.
 * Validation internals (magic-byte etc.) are covered by UploadServiceTest; this
 * class verifies the HTTP status/ProblemDetail contract.
 */
@WebMvcTest({UploadController.class, UploadExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class UploadControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UploadService service;

    @Test
    void upload_valid_file_returns_201_with_public_url() throws Exception {
        when(service.store(any(), anyString()))
                .thenReturn(new UploadResponseDto("/uploads/projects/uuid.jpg", "uuid.jpg", "image/jpeg", 12345));
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/admin/uploads").file(file).param("category", "project-image"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/uploads/projects/uuid.jpg"));
    }

    @Test
    void upload_missing_category_returns_400() throws Exception {
        when(service.store(any(), anyString())).thenThrow(new MissingUploadCategoryException(null));
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/admin/uploads").file(file).param("category", "bogus"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_wrong_type_returns_400() throws Exception {
        when(service.store(any(), anyString())).thenThrow(new UnsupportedFileTypeException("bad type"));
        var file = new MockMultipartFile("file", "photo.exe", "application/octet-stream", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/admin/uploads").file(file).param("category", "project-image"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Unsupported File Type"));
    }

    @Test
    void upload_too_large_returns_413() throws Exception {
        when(service.store(any(), anyString())).thenThrow(new FileTooLargeException("too big"));
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/admin/uploads").file(file).param("category", "project-image"))
                .andExpect(status().isPayloadTooLarge());
    }
}
