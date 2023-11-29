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
public class ZenodoRestGrant {
    private String code;
    private String internal_id;
    private ZenodoRestGrantFunder funder;
    private String title;
    private String acronym;
    private String program;
    private String url;
}
