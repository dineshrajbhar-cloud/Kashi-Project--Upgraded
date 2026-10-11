package com.kashi.portal.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/** Builds a safe Pageable from request parameters. Only whitelisted sort fields are accepted. */
public final class Paging {

    public static final int MAX_PAGE_SIZE = 50;

    private Paging() {
    }

    /**
     * @param sort a value such as "name,asc" or "createdAt,desc"; the direction is optional
     */
    public static Pageable of(int page, int size, String sort, Set<String> allowedSortFields) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, parseSort(sort, allowedSortFields));
    }

    private static Sort parseSort(String sort, Set<String> allowedSortFields) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!allowedSortFields.contains(field)) {
            throw new BadRequestException("Cannot sort by '" + field + "'. Allowed fields: "
                    + String.join(", ", new TreeSet<>(allowedSortFields)));
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> new BadRequestException("Sort direction must be 'asc' or 'desc'"));
        }

        // The id tie-breaker keeps page boundaries stable when many rows share the same sort value
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
