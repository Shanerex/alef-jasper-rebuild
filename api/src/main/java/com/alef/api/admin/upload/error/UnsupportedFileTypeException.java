package com.alef.api.admin.upload.error;

/**
 * Thrown when an uploaded file's content-type/extension/magic-bytes don't
 * match its category's allowed set (design.md §A.6, F12-AC22) -- maps to 400.
 */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
