package de.vzg.oai_importer.foreign.zenodo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZenodoRestPerson {
    private String name;
    private String affiliation;
    private String orcid;
    private String type;
}
