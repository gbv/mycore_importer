package de.vzg.oai_importer.foreign.zenodo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZenodoRestMetadata {
    private String title;
    private String doi;
    private String publication_date;

    private String description;

    private String access_right;

    private List<ZenodoRestPerson> creators;

    private List<ZenodoRestPerson> contributors;

    private List<String> keywords;

    private List<ZenodoRestSubjects> subjects;

    private ZenodoRestJournal journal;

    private ZenodoRestLicense license;

    private List<ZenodoRestGrant> grants;

    private ZenodoRestRelations relations;

    private ZenodoRestResourceType resource_type;

}
