package de.vzg.oai_importer.foreign.zenodo;

import de.vzg.oai_importer.foreign.Configuration;
import lombok.Data;

@Data
public class ZenodoSourceConfiguration implements Configuration {

    private String url;

    private String community;

    @Override
    public String getHarvester() {
        return ZenodoHarvester.ZENODO_HARVESTER;
    }

    @Override
    public String getName() {
        return url + " " + community;
    }
}
