package de.vzg.oai_importer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.oai.OAISourceConfiguration;
import de.vzg.oai_importer.foreign.ppnlist.PPNListConfiguration;
import de.vzg.oai_importer.foreign.zenodo.ZenodoSourceConfiguration;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import lombok.Data;

@ConfigurationProperties(prefix = "importer")
@Data
public class ImporterConfiguration {

    private Map<String, OAISourceConfiguration> oaiSources;

    private Map<String, ZenodoSourceConfiguration> zenodoSources;

    private Map<String, PPNListConfiguration> ppnLists;

    private Map<String, MyCoReTargetConfiguration> targets;

    private Map<String, ImportJobConfiguration> jobs;

    public HashMap<String, Configuration> getCombinedConfig() {
        Map<String, OAISourceConfiguration> oaiSources = Optional.ofNullable( getOaiSources())
                .orElseGet(Collections::emptyMap);

        Map<String, ZenodoSourceConfiguration> zenodoSources = Optional.ofNullable(getZenodoSources())
                .orElseGet(Collections::emptyMap);

        Map<String, PPNListConfiguration> ppnLists = Optional.ofNullable(getPpnLists())
                .orElseGet(Collections::emptyMap);

        HashMap<String, Configuration> sourceMap = new HashMap<>();

        sourceMap.putAll(oaiSources);
        sourceMap.putAll(zenodoSources);
        sourceMap.putAll(ppnLists);

        return sourceMap;
    }

}
