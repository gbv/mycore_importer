package de.vzg.oai_importer.foreign.oai;

import de.vzg.oai_importer.foreign.Configuration;
import lombok.Data;

@Data
public class OAISourceConfiguration implements Configuration {

    private String url;

    private String metadataPrefix;

    private String set;

    @Override
    public String getHarvester() {
        return OAIDatabaseHarvester.OAI_HARVESTER;
    }

    @Override
    public String getName() {
        return url + " " + metadataPrefix + " " + set;
    }
}
