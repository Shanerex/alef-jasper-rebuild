package com.alef.api.admin.upload;

import com.alef.api.admin.upload.error.FileTooLargeException;
import com.alef.api.admin.upload.error.MissingUploadCategoryException;
import com.alef.api.admin.upload.error.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for UploadService (architecture §2.4, design §A.6, F12-AC20..AC22).
 *
 * Risk-weighted per the handoff test list: type/size/magic-byte validation is
 * the specific behaviour called out for coverage. Uses a real @TempDir so the
 * write-to-disk path is exercised for real, not mocked away.
 */
class UploadServiceTest {

    /** Minimal valid PNG signature (8 bytes) plus a little padding -- enough for the magic-byte sniff. */
    private static final byte[] PNG_HEAD = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };

    /** Minimal valid JPEG signature. */
    private static final byte[] JPEG_HEAD = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    /** Minimal valid PDF signature ("%PDF"). */
    private static final byte[] PDF_HEAD = {'%', 'P', 'D', 'F', '-', '1', '.', '4', 0, 0, 0, 0};

    @TempDir
    Path tempDir;

    private UploadService service(Path dir) {
        return new UploadService(dir.toString());
    }

    @Test
    void stores_a_valid_png_project_image_and_returns_its_public_url() {
        UploadService service = service(tempDir);
        var file = new MockMultipartFile("file", "photo.png", "image/png", PNG_HEAD);

        var result = service.store(file, "project-image");

        assertThat(result.url()).startsWith("/uploads/projects/");
        assertThat(result.url()).endsWith(".png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(Files.exists(tempDir.resolve("projects").resolve(result.filename()))).isTrue();
    }

    @Test
    void stores_a_valid_jpeg_team_photo_under_the_team_subfolder() {
        UploadService service = service(tempDir);
        var file = new MockMultipartFile("file", "headshot.jpg", "image/jpeg", JPEG_HEAD);

        var result = service.store(file, "team-photo");

        assertThat(result.url()).startsWith("/uploads/team/");
    }

    @Test
    void stores_a_valid_pdf_sample_file_under_samples_files() {
        UploadService service = service(tempDir);
        var file = new MockMultipartFile("file", "bbs-sample.pdf", "application/pdf", PDF_HEAD);

        var result = service.store(file, "sample-file");

        assertThat(result.url()).startsWith("/uploads/samples/files/");
    }

    @Test
    void unknown_category_throws_missing_category_exception() {
        UploadService service = service(tempDir);
        var file = new MockMultipartFile("file", "photo.png", "image/png", PNG_HEAD);

        assertThatThrownBy(() -> service.store(file, "not-a-real-category"))
                .isInstanceOf(MissingUploadCategoryException.class);
    }

    @Test
    void wrong_content_type_for_category_is_rejected() {
        UploadService service = service(tempDir);
        // application/pdf is not an allowed type for project-image.
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", PDF_HEAD);

        assertThatThrownBy(() -> service.store(file, "project-image"))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void wrong_extension_for_category_is_rejected_even_with_a_matching_content_type_header() {
        UploadService service = service(tempDir);
        // Declared content type says image/png, but the extension is .exe -- reject.
        var file = new MockMultipartFile("file", "malware.exe", "image/png", PNG_HEAD);

        assertThatThrownBy(() -> service.store(file, "project-image"))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void magic_byte_mismatch_is_rejected_even_with_matching_content_type_and_extension() {
        UploadService service = service(tempDir);
        // Declared PNG, .png extension, but the actual bytes are not a PNG signature
        // (spoofed content-type/extension -- architecture §2.4's whole reason for
        // the magic-byte check).
        byte[] notActuallyPng = "this is not an image".getBytes();
        var file = new MockMultipartFile("file", "fake.png", "image/png", notActuallyPng);

        assertThatThrownBy(() -> service.store(file, "project-image"))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void oversized_image_is_rejected_with_a_413_style_exception() {
        UploadService service = service(tempDir);
        byte[] oversized = new byte[6 * 1024 * 1024]; // 6 MB > 5 MB image cap
        System.arraycopy(PNG_HEAD, 0, oversized, 0, PNG_HEAD.length);
        var file = new MockMultipartFile("file", "huge.png", "image/png", oversized);

        assertThatThrownBy(() -> service.store(file, "project-image"))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void pdf_up_to_twenty_five_mb_is_accepted_for_sample_file() {
        UploadService service = service(tempDir);
        byte[] justUnderCap = new byte[24 * 1024 * 1024]; // 24 MB < 25 MB PDF cap
        System.arraycopy(PDF_HEAD, 0, justUnderCap, 0, PDF_HEAD.length);
        var file = new MockMultipartFile("file", "big.pdf", "application/pdf", justUnderCap);

        var result = service.store(file, "sample-file");

        assertThat(result.size()).isEqualTo(justUnderCap.length);
    }
}
