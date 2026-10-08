package de.vzg.oai_importer.view;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Helper for formatting values inside jte templates.
 */
public final class FormatHelper {

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMANY);

    private FormatHelper() {
    }

    /**
     * Formats a timestamp for display, as stored, without converting the offset.
     *
     * @param value the timestamp, may be null
     * @return the formatted timestamp or an empty string
     */
    public static String dateTime(OffsetDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    /**
     * Combines a count with the matching German singular or plural noun.
     *
     * @param count the number of items
     * @param singular the noun in singular, for example "Datensatz"
     * @param plural the noun in plural, for example "Datensätze"
     * @return the count followed by the matching noun
     */
    public static String count(long count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    /**
     * Renders a flag as a German yes or no.
     *
     * @param value the flag, may be null
     * @return "Ja" if the flag is set, otherwise "Nein"
     */
    public static String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Ja" : "Nein";
    }
}
