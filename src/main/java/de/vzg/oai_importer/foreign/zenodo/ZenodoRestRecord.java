package de.vzg.oai_importer.foreign.zenodo;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ZenodoRestRecord {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant created;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant modified;

    private long id;

    private String doi;

    private String conceptdoi;

    private ZenodoRestMetadata metadata;

    private String title;

    private List<ZenodoRestFileMetadata> files;

    private ZenodoRestLinks links;

}
