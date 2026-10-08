package de.vzg.oai_importer;

import java.util.List;

/**
 * Outcome of a job that imports or updates several records one after another.
 *
 * @param action what the job did with the records
 * @param total the number of records the job tried to process
 * @param errorRecords the foreign ids of the records that could not be processed
 */
public record JobResult(Action action, long total, List<String> errorRecords) {

    public JobResult {
        errorRecords = List.copyOf(errorRecords);
    }

    /**
     * @return true if every record was processed without an error
     */
    public boolean isSuccess() {
        return errorRecords.isEmpty();
    }

    /**
     * The kind of work a job did with its records.
     */
    public enum Action {
        IMPORT, UPDATE
    }
}
