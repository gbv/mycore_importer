package de.vzg.oai_importer.mycore.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyCoReObjectQuery {

    public MyCoReObjectQuery() {
        numberGreater = -1;
        limit = -1;
        offset = -1;
        afterId = null;
        type = null;
        project = null;
        status = null;
        numberLess = -1;
    }

    private String afterId;

    private int offset;

    private int limit;

    private int numberGreater;

    private int numberLess;

    private String type;

    private String project;

    private String status;

    private SortBy sortBy;

    private SortOrder sortOrder;

    private Instant modifiedBefore;

    private Instant modifiedAfter;

    private Instant createdBefore;

    private Instant createdAfter;

    private Instant deletedBefore;

    private Instant deletedAfter;

    private String createdBy;

    private String modifiedBy;

    private String deletedBy;

    private final List<String> includeCategories = new ArrayList<>();

    public enum SortBy {
        id,
        modified,
        created
    }

    public enum SortOrder {
        asc,
        desc
    }
}
