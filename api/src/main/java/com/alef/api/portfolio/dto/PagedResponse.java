package com.alef.api.portfolio.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Explicit pagination envelope matching the architecture 3.1 contract.
 *
 * Uses a custom shape instead of Spring's Page serialization, which emits
 * an unstable, verbose payload. The architecture fixes exactly five fields:
 * content, page, size, totalElements, totalPages.
 *
 * @param <T> the type of items in the content list
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Converts a Spring Data Page into the stable API envelope.
     * Keeps the mapping in one place so all paginated endpoints
     * produce the same shape.
     */
    public static <T> PagedResponse<T> from(Page<T> springPage) {
        return new PagedResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }
}
