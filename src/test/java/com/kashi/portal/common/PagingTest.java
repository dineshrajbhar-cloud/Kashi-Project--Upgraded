package com.kashi.portal.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PagingTest {

    private static final Set<String> FIELDS = Set.of("name", "createdAt");

    @Test
    void pageSizeIsCappedAtTheMaximum() {
        Pageable pageable = Paging.of(0, 500, "name,asc", FIELDS);

        assertEquals(Paging.MAX_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    void negativePageAndZeroSizeAreCorrected() {
        Pageable pageable = Paging.of(-3, 0, "name", FIELDS);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(1, pageable.getPageSize());
    }

    @Test
    void sortDirectionDefaultsToAscending() {
        Pageable pageable = Paging.of(0, 10, "name", FIELDS);

        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void descendingSortIsAppliedWithAnIdTieBreaker() {
        Pageable pageable = Paging.of(2, 10, "createdAt,desc", FIELDS);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void unknownSortFieldIsRejectedAndNamesTheAllowedFields() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> Paging.of(0, 10, "password,asc", FIELDS));

        assertEquals("Cannot sort by 'password'. Allowed fields: createdAt, name", ex.getMessage());
    }

    @Test
    void unknownSortDirectionIsRejected() {
        assertThrows(BadRequestException.class, () -> Paging.of(0, 10, "name,sideways", FIELDS));
    }
}
