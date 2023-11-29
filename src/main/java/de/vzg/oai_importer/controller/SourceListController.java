package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import de.vzg.oai_importer.foreign.oai.OAISourceConfiguration;
import de.vzg.oai_importer.foreign.zenodo.ZenodoSourceConfiguration;

@Controller
@RequestMapping("/sources")
@PreAuthorize("hasAnyAuthority('source')")
public class SourceListController {

    @Autowired
    private ImporterConfiguration configuration;

    @Autowired
    private ForeignEntityRepository recordRepository;

    @Autowired
    private ApplicationContext applicationContext;


    @GetMapping("/")
    public String listSources(Model model) {
        HashMap<String, Configuration> sourceMap = getCombinedConfig();

        model.addAttribute("sources", sourceMap);
        return "sources_list_config";
    }

    private HashMap<String, Configuration> getCombinedConfig() {
        Map<String, OAISourceConfiguration> oaiSources =Optional.ofNullable( configuration.getOaiSources())
                .orElseGet(Collections::emptyMap);

        Map<String, ZenodoSourceConfiguration> zenodoSources = Optional.ofNullable(configuration.getZenodoSources())
                .orElseGet(Collections::emptyMap);

        HashMap<String, Configuration> sourceMap = new HashMap<>();

        sourceMap.putAll(oaiSources);
        sourceMap.putAll(zenodoSources);
        return sourceMap;
    }

    @GetMapping("/{source}/")
    @PreAuthorize("hasAnyAuthority('source-' + #sourceId)")
    public String showSource(@PathVariable("source") String sourceId, Model model) {
        Configuration source = getCombinedConfig().get(sourceId);

        List<ForeignEntity> records
            = recordRepository.findAllByConfigIdAndIsDeletedOrderByDatestampDesc(sourceId, false);

        model.addAttribute("source", source);
        model.addAttribute("records", records);

        return "source_records";
    }

    @GetMapping("/{source}/update")
    @PreAuthorize("hasAnyAuthority('source-' + #sourceId)")
    public String updateSource(@PathVariable("source") String sourceId, Model model) throws IOException, URISyntaxException {
        Configuration source = getCombinedConfig().get(sourceId);

        Harvester<Configuration> bean = (Harvester<Configuration>) applicationContext.getBean(source.getHarvester());
        List<ForeignEntity> updatedRecords = bean.update(sourceId, source);
        model.addAttribute("records", updatedRecords);

        return "source_records";
    }
}
