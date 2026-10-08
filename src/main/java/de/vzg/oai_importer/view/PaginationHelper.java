package de.vzg.oai_importer.view;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.springframework.data.domain.Page;

/**
 * Builds the page numbers shown in the pager.
 */
public final class PaginationHelper {

    /**
     * Marks a gap between two page numbers. Not a valid page number, pages are counted from one.
     */
    public static final int GAP = 0;

    /**
     * How many pages are shown on each side of the current page.
     */
    private static final int NEIGHBOURS = 2;

    private PaginationHelper() {
    }

    /**
     * Builds the window of page numbers for a pager: the first page, the pages around the current
     * one, and the last page. Larger distances are collapsed into a {@link #GAP} marker, so the
     * pager stays at nine entries at most, no matter how many pages there are.
     *
     * @param page the page being rendered
     * @return the page numbers to render, counted from one, with {@link #GAP} for a gap
     */
    public static List<Integer> pageWindow(Page<?> page) {
        int totalPages = page.getTotalPages();
        if (totalPages < 1) {
            return List.of();
        }

        int current = page.getNumber() + 1;
        TreeSet<Integer> numbers = new TreeSet<>();
        numbers.add(1);
        numbers.add(totalPages);
        for (int number = current - NEIGHBOURS; number <= current + NEIGHBOURS; number++) {
            if (number >= 1 && number <= totalPages) {
                numbers.add(number);
            }
        }

        List<Integer> window = new ArrayList<>();
        int previous = 0;
        for (int number : numbers) {
            if (previous != 0) {
                int skipped = number - previous - 1;
                if (skipped == 1) {
                    // A single skipped page takes no more room than the gap marker.
                    window.add(previous + 1);
                } else if (skipped > 1) {
                    window.add(GAP);
                }
            }
            window.add(number);
            previous = number;
        }

        return window;
    }
}
