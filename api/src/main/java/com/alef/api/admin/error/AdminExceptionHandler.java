package com.alef.api.admin.error;

import com.alef.api.admin.project.AdminProjectController;
import com.alef.api.admin.sample.AdminSampleController;
import com.alef.api.admin.team.AdminTeamController;
import com.alef.api.admin.trust.AdminTrustController;
import com.alef.api.admin.upload.UploadController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps the shared admin CRUD exceptions to RFC 9457 ProblemDetail responses
 * (design.md "Common error shapes"), scoped to the four admin resource
 * controllers plus uploads so this one advice class covers the whole admin
 * CRUD surface without a parallel handler per resource.
 */
@RestControllerAdvice(assignableTypes = {
        AdminProjectController.class,
        AdminTeamController.class,
        AdminTrustController.class,
        AdminSampleController.class,
        UploadController.class
})
public class AdminExceptionHandler {

    /** Unknown {id} on GET/PUT/DELETE -> 404. */
    @ExceptionHandler(AdminResourceNotFoundException.class)
    public ProblemDetail handleNotFound(AdminResourceNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Not Found");
        return detail;
    }

    /** slug / item_key already exists -> 409. */
    @ExceptionHandler(AdminDuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(AdminDuplicateKeyException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Conflict");
        return detail;
    }

    /** Value outside a DEC-009 app-layer vocabulary -> 400, field-mapped like Bean Validation. */
    @ExceptionHandler(AdminInvalidVocabularyException.class)
    public ProblemDetail handleInvalidVocabulary(AdminInvalidVocabularyException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Validation Failed");
        detail.setProperty("fields", Map.of(ex.getField(), List.of(ex.getMessage())));
        return detail;
    }

    /** Bean Validation failure (missing required field, bad format) -> 400 with a field map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields failed validation.");
        detail.setTitle("Validation Failed");
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())));
        detail.setProperty("fields", fieldErrors);
        return detail;
    }
}
