package com.alef.api.admin.upload;

import com.alef.api.admin.upload.dto.UploadResponseDto;
import com.alef.api.admin.upload.error.FileTooLargeException;
import com.alef.api.admin.upload.error.MissingUploadCategoryException;
import com.alef.api.admin.upload.error.UnsupportedFileTypeException;
import com.alef.api.admin.upload.vocabulary.MediaKind;
import com.alef.api.admin.upload.vocabulary.UploadCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Validates and stores an uploaded file on the local filesystem volume
 * (architecture §2.4, DEC-022, design.md §A.6, F12-AC20..AC22).
 *
 * Three layers of validation, all required (declared Content-Type alone is
 * client-controlled and spoofable -- architecture §2.4):
 * 1. category is a recognized UploadCategory wire value.
 * 2. declared Content-Type AND file extension are both in the category's
 *    MediaKind allow-list.
 * 3. a magic-byte sniff of the leading bytes matches the MediaKind.
 * Size is checked against the category's per-kind cap (Spring's global
 * multipart max-file-size is the first, coarser gate -- see application.yml).
 *
 * Filenames are randomized (UUID) so the client never controls the stored
 * path -- this rules out path traversal and filename collisions/leakage
 * (architecture §2.4).
 */
@Service
public class UploadService {

    /** Number of leading bytes read for the magic-byte sniff (covers the longest signature, WEBP's 12). */
    private static final int MAGIC_BYTES_TO_READ = 12;

    private final Path uploadRoot;

    public UploadService(@Value("${alef.upload-dir:/var/alef/uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir);
    }

    /**
     * Validates and persists the given multipart file under its category's
     * subfolder, returning the public-relative URL to store on a record.
     *
     * @throws MissingUploadCategoryException if category is missing/unrecognized
     * @throws UnsupportedFileTypeException   if content-type/extension/magic-bytes don't match
     * @throws FileTooLargeException          if the file exceeds the category's size cap
     */
    public UploadResponseDto store(MultipartFile file, String categoryValue) {
        UploadCategory category = UploadCategory.fromWireValue(categoryValue)
                .orElseThrow(() -> new MissingUploadCategoryException(categoryValue));
        MediaKind kind = category.mediaKind();

        String contentType = normalize(file.getContentType());
        String extension = extractExtension(file.getOriginalFilename());

        if (contentType == null || !kind.getAllowedContentTypes().contains(contentType)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported content type '%s'. Allowed: %s".formatted(contentType, kind.getAllowedContentTypes()));
        }
        if (extension == null || !kind.getAllowedExtensions().contains(extension)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file extension. Allowed: %s".formatted(kind.getAllowedExtensions()));
        }
        if (file.getSize() > kind.getMaxSizeBytes()) {
            throw new FileTooLargeException(
                    "File exceeds the %d MB limit for this category.".formatted(kind.getMaxSizeBytes() / (1024 * 1024)));
        }
        if (!kind.magicBytesMatch(readHead(file))) {
            throw new UnsupportedFileTypeException("File content does not match its declared type.");
        }

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetDir = uploadRoot.resolve(category.subfolder());
        Path target = targetDir.resolve(storedFilename);
        writeToDisk(file, targetDir, target);

        String url = "/uploads/" + category.subfolder() + "/" + storedFilename;
        return new UploadResponseDto(url, storedFilename, contentType, file.getSize());
    }

    /** Reads the first MAGIC_BYTES_TO_READ bytes of the upload for the type sniff, without consuming the whole stream twice. */
    private byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[MAGIC_BYTES_TO_READ];
            int read = in.readNBytes(head, 0, MAGIC_BYTES_TO_READ);
            return read == MAGIC_BYTES_TO_READ ? head : java.util.Arrays.copyOf(head, read);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    /** Creates the category subfolder if needed and copies the multipart bytes to the target path. */
    private void writeToDisk(MultipartFile file, Path targetDir, Path target) {
        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    /**
     * Extracts the lowercase extension from an original filename, or null if
     * there isn't one. Only used for allow-list membership -- never trusted
     * as the actual type (that's the magic-byte check's job).
     */
    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
