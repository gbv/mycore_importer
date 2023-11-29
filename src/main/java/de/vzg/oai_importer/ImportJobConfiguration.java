package de.vzg.oai_importer;

import java.util.Map;

import lombok.Data;

@Data
public class ImportJobConfiguration {

    /**
     * The source to harvest
     */
    private String sourceConfigId;

    /**
     * The target to import to
     */
    private String targetConfigId;

    /**
     * The importer to use (bean name)
     */
    private String importer;

    private Map<String, String> importerConfig;

}
