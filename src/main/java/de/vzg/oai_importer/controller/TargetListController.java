package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String showTarget(@PathVariable String targetID, Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "100") int size) {
        MyCoReTargetConfiguration target = configuration.getTargets().get(targetID);
        Page<MyCoReObjectInfo> objects = objectInfoRepository.findByRepository(target.getUrl(),
                Pageable.ofSize(size).withPage(page));
        model.addAttribute("target", targetID);
        model.addAttribute("records", objects);
        model.addAttribute("pages", IntStream.rangeClosed(1, objects.getTotalPages())
                .boxed());
        return "target_objects";
    }

    @RequestMapping("/{targetID}/update")
    @PreAuthorize("hasAnyAuthority('target-' + #targetID)")
    public String updateTarget(@PathVariable String targetID, Model model) throws IOException, URISyntaxException {
        MyCoReTargetConfiguration target = configuration.getTargets().get(targetID);

        List<MyCoReObjectInfo> objects = synchronizeService.synchronize(target);
        model.addAttribute("target", targetID);
        model.addAttribute("records", new PageImpl<>(objects));
        model.addAttribute("pages", IntStream.rangeClosed(1, 1).boxed());
        return "target_objects";
    }
}
