package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.mycore.MyCoReSynchronizeService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfoRepository;

@Controller
@RequestMapping("/targets")
@PreAuthorize("hasAnyAuthority('target')")
public class TargetListController {

    @Autowired
    private ImporterConfiguration configuration;

    @Autowired
    private MyCoReObjectInfoRepository objectInfoRepository;

    @Autowired
    private MyCoReSynchronizeService synchronizeService;

    @RequestMapping("/")
    public String listTargets(Model model) {
        model.addAttribute("targets", configuration.getTargets());
        return "target_list_config";
    }

    @RequestMapping("/{targetID}/")
    @PreAuthorize("hasAnyAuthority('target-' + #targetID)")
    public String showTarget(@PathVariable String targetID, Model model) {
        MyCoReTargetConfiguration target = configuration.getTargets().get(targetID);
        List<MyCoReObjectInfo> objects = objectInfoRepository.findByRepository(target.getUrl());
        model.addAttribute("objects", objects);
        return "target_objects";
    }

    @RequestMapping("/{targetID}/update")
    @PreAuthorize("hasAnyAuthority('target-' + #targetID)")
    public String updateTarget(@PathVariable String targetID, Model model) throws IOException, URISyntaxException {
        MyCoReTargetConfiguration target = configuration.getTargets().get(targetID);

        List<MyCoReObjectInfo> objects = synchronizeService.synchronize(target);
        model.addAttribute("objects", objects);

        return "target_objects";
    }
}
