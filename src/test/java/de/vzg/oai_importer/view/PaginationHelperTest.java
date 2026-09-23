package de.vzg.oai_importer.view;

import static de.vzg.oai_importer.view.PaginationHelper.GAP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class PaginationHelperTest {

    /**
     * @param pageIndex the page to show, counted from zero
     * @param totalPages the number of pages
     */
    private Page<String> page(int pageIndex, int totalPages) {
        int size = 10;
        return new PageImpl<>(List.of("entry"), PageRequest.of(pageIndex, size), (long) totalPages * size);
    }

    @Test
    public void showsEveryPageWhenThereAreFew() {
        assertEquals(List.of(1, 2, 3, 4, 5), PaginationHelper.pageWindow(page(0, 5)));
    }

    @Test
    public void collapsesTheTailWhenOnTheFirstPage() {
        assertEquals(List.of(1, 2, 3, GAP, 2500), PaginationHelper.pageWindow(page(0, 2500)));
    }

    @Test
    public void collapsesBothSidesInTheMiddle() {
        assertEquals(List.of(1, GAP, 40, 41, 42, 43, 44, GAP, 2500),
            PaginationHelper.pageWindow(page(41, 2500)));
    }

    @Test
    public void collapsesTheHeadWhenOnTheLastPage() {
        assertEquals(List.of(1, GAP, 2498, 2499, 2500), PaginationHelper.pageWindow(page(2499, 2500)));
    }

    @Test
    public void showsASkippedSinglePageInsteadOfAGap() {
        // Between 1 and 3 only page 2 is missing, which takes no more room than the marker.
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), PaginationHelper.pageWindow(page(4, 7)));
    }

    @Test
    public void staysWithinNineEntriesForAnyPage() {
        for (int index = 0; index < 2500; index++) {
            List<Integer> window = PaginationHelper.pageWindow(page(index, 2500));
            assertTrue(window.size() <= 9, "Seite " + (index + 1) + " ergab " + window.size() + " Einträge");
        }
    }

    @Test
    public void showsTheSinglePageOfAnUnpagedResult() {
        // An unpaged PageImpl reports one page, not zero.
        assertEquals(List.of(1), PaginationHelper.pageWindow(new PageImpl<>(List.of())));
    }

    @Test
    public void handlesAnEmptyResult() {
        Page<String> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        assertEquals(0, empty.getTotalPages());
        assertEquals(List.of(), PaginationHelper.pageWindow(empty));
    }
}
