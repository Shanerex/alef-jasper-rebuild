package com.alef.api.admin.upload.error;

import com.alef.api.admin.upload.UploadController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Maps upload-specific exceptions to RFC 9457 ProblemDetail responses
 * (design.md §A.6 error table), scoped to UploadController only.
 */
@RestControllerAdvice(assignableTypes = UploadController.class)
public class UploadExceptionHandler {

    /** category missing/unknown -> 400. */
    @ExceptionHandler(MissingUploadCategoryException.class)
    public ProblemDetail handleMissingCategory(MissingUploadCategoryException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /** content-type/extension/magic-bytes mismatch -> 400. */
    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ProblemDetail handleUnsupportedType(UnsupportedFileTypeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Unsupported File Type", ex.getMessage());
    }

    /** Per-category size cap exceeded (service-level re-check) -> 413. */
    @ExceptionHandler(FileTooLargeException.class)
    public ProblemDetail handleTooLarge(FileTooLargeException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", ex.getMessage());
    }

    /**
     * Spring's global multipart max-file-size/max-request-size cap (the
     * first, coarser gate -- application.yml) -> 413.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleSpringSizeLimit(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", "The uploaded file is too large.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
