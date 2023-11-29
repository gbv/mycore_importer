package de.vzg.oai_importer;

import de.vzg.oai_importer.foreign.zenodo.ZenodoSourceConfiguration;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.foreign.oai.OAISourceConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "importer")
@Data
public class ImporterConfiguration {

    private Map<String, OAISourceConfiguration> oaiSources;

    private Map<String, ZenodoSourceConfiguration> zenodoSources;

    private Map<String, MyCoReTargetConfiguration> targets;

    private Map<String, ImportJobConfiguration> jobs;

}
