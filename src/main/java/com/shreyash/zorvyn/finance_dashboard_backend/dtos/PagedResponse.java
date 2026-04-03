package com.shreyash.zorvyn.finance_dashboard_backend.dtos;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic wrapper for paginated results.
 * Example:
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 42,
 *   "totalPages": 5,
 *   "last": false,
 *   "first": true
 * }
 */

@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;

    /**
     * Convenience factory: builds a PagedResponse<T> directly from a Spring
     * Data {@link Page} and a pre-mapped content list.
     *
     * @param pageData Spring Page (provides pagination metadata)
     * @param content  already-mapped list of response objects
     */
    public static <T> PagedResponse<T> from(Page<?> pageData, List<T> content) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .first(pageData.isFirst())
                .build();
    }
}
