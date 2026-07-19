package com.alef.api.admin.upload.error;

/**
 * Thrown when the multipart `category` part is missing or not one of the
 * four recognized UploadCategory wire values (design.md §A.6) -- maps to 400.
 */
public class MissingUploadCategoryException extends RuntimeException {

    public MissingUploadCategoryException(String category) {
        super(category == null
                ? "category is required"
                : "Unknown upload category: " + category);
    }
}
