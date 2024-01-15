package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;

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
        HashMap<String, Configuration> sourceMap = configuration.getCombinedConfig();

        model.addAttribute("sources", sourceMap);
        return "sources_list_config";
    }


    @GetMapping("/{source}/")
    @PreAuthorize("hasAnyAuthority('source-' + #sourceId)")
    public String showSource(@PathVariable("source") String sourceId,
                             Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "100") int size) {
        Configuration source = configuration.getCombinedConfig().get(sourceId);


        Page<ForeignEntity> records
                = recordRepository.findAllByConfigIdAndIsDeletedOrderByDatestampDesc(sourceId, false,
                Pageable.ofSize(size).withPage(page));
        model.addAttribute("source", sourceId);
        model.addAttribute("records", records);
        model.addAttribute("pages", IntStream.rangeClosed(1, records.getTotalPages())
                .boxed());
        return "source_records";
    }

    @GetMapping("/{source}/update")
    @PreAuthorize("hasAnyAuthority('source-' + #sourceId)")
    public String updateSource(@PathVariable("source") String sourceId, Model model) throws IOException, URISyntaxException {
        Configuration source = configuration.getCombinedConfig().get(sourceId);

        Harvester<Configuration> bean = (Harvester<Configuration>) applicationContext.getBean(source.getHarvester());
        List<ForeignEntity> updatedRecords = bean.update(sourceId, source);
        model.addAttribute("records", new PageImpl<ForeignEntity>(updatedRecords));
        model.addAttribute("source", sourceId);
        model.addAttribute("pages", IntStream.rangeClosed(1, 1));
        return "source_records";
    }
}
