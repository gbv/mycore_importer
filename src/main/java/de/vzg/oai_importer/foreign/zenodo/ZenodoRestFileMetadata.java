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
public class ZenodoRestFileMetadata {

    private String id;

    private String key;

    private long size;

    private String checksum;

    private ZenodoRestLinks links;

}
