package com.alef.api.admin.upload.error;

/**
 * Thrown when an uploaded file exceeds its category's per-kind size cap
 * (design.md §A.6, F12-AC22) -- maps to 413. Spring's global multipart
 * max-file-size is the first gate (also 413); this is the service's
 * per-category re-check (architecture §2.4: images 5 MB, PDF 25 MB).
 */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
