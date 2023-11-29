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
public class ZenodoRestLinks {

    private String self;
    private String self_html;
    private String self_doi;
    private String doi;
    private String parent;
    private String parent_html;
    private String parent_doi;
    private String self_iiif_manifest;
    private String self_iiif_sequence;
    private String files;
    private String media_files;

}
