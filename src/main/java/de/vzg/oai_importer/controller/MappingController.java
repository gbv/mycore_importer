package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.mapping.MappingService;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mapping.jpa.MappingGroup;
import de.vzg.oai_importer.mycore.MyCoReRestAPIService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;

@Controller
@RequestMapping("/mapping")
@PreAuthorize("hasAnyAuthority('mapping')")
public class MappingController {

    @Autowired
    MappingService mappingService;

    @Autowired
    MyCoReRestAPIService myCoReRestAPIService;

    @Autowired
    ImporterConfiguration configuration;

    @RequestMapping("/groups/")
    public String listGroups(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
        Model model) {
        Page<MappingGroup> customerPage = mappingService.getMappingGroups(page, size);

        model.addAttribute("groups", customerPage);
        model.addAttribute("pages", IntStream.rangeClosed(1, customerPage.getTotalPages())
                .boxed()
            .collect(Collectors.toList()));
        return "mapping/mapping_groups";
    }

    @RequestMapping(value = "/groups/{id}/edit/", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin')")
    public String editGroup(@PathVariable(name = "id") Long id, Model model) {
        model.addAttribute("group", mappingService.getGroup(id));
        model.addAttribute("targets", configuration.getTargets().keySet());
        return "mapping/edit_group";
    }

    @RequestMapping(value = "/groups/{id}/edit/", method = RequestMethod.POST)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin')")
    public String editGroup(@PathVariable(name = "id") Long id,
        @RequestParam("name") String name,
        @RequestParam("description") String description,
                            @RequestParam("target") String target,
                            @RequestParam("classId") String classId,
        Model model) {
        if (classId == null || classId.isEmpty()) {
            classId = null;
        }
        if(target == null || target.isEmpty()) {
            target = null;
        }

        MappingGroup mappingGroup = mappingService.updateGroup(id, name, description, target, classId);
        return "redirect:/mapping/groups/" + mappingGroup.getId() + "/";
    }

    @RequestMapping(value = "/groups/{id}/", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin', 'mapping-' + #id)")
    public String listMappings(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
        @PathVariable(name = "id") Long id, Model model) {
        MappingGroup group = mappingService.getGroup(id);
        Page<Mapping> mappings = mappingService.getMappingsByGroup(group, page, size);
        model.addAttribute("mappings", mappings);
        model.addAttribute("group", group);
        return "mapping/mapping_list";
    }

    @RequestMapping(value = "/groups/add/", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin')")
    public String addGroup(Model model) {
        model.addAttribute("targets", configuration.getTargets().keySet());
        return "mapping/edit_group";
    }

    @RequestMapping(value = "/groups/add/", method = RequestMethod.POST)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin')")
    public String addGroup(@RequestParam("name") String name,
        @RequestParam("description") String description,
        @RequestParam("target") String target,
        @RequestParam("classId") String classId,
        Model model) {
        if (classId == null || classId.isEmpty()) {
            classId = null;
        }

        MappingGroup mappingGroup = mappingService.addGroup(name, description, target, classId);
        return "redirect:/mapping/groups/" + mappingGroup.getId() + "/";
    }

    @RequestMapping(value = "/groups/{id}/add/", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin', 'mapping-' + #id)")
    public String addMapping(@PathVariable(name = "id") Long id, Model model) throws IOException, URISyntaxException {
        MappingGroup group = mappingService.getGroup(id);
        model.addAttribute("group", group);

        if(!(group.getTarget() == null || group.getClassId() == null)) {
            MyCoReTargetConfiguration targetConfiguration = configuration.getTargets().get(group.getTarget());
            List<MyCoReRestAPIService.Category> categories = myCoReRestAPIService.getClassificationCategories(targetConfiguration, group.getClassId());
            model.addAttribute("categories", categories);
        }

        return "mapping/edit_mapping";
    }

    @RequestMapping(value = "/groups/{id}/add/", method = RequestMethod.POST)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin', 'mapping-' + #id)")
    public String addMapping(@PathVariable(name = "id") Long id,
        @RequestParam("from") String from,
        @RequestParam("to") String to,
        Model model) {
        MappingGroup group = mappingService.getGroup(id);
        Mapping mapping = mappingService.addMapping(group, from, to);
        return "redirect:/mapping/groups/" + group.getId() + "/";
    }

    @RequestMapping(value = "/groups/{gid}/{mid}/edit/", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin', 'mapping-' + #gid)")
    public String editMapping(@PathVariable(name = "gid") Long gid, @PathVariable(name = "mid") Long mid, Model model) throws IOException, URISyntaxException {
        MappingGroup group = mappingService.getGroup(gid);
        model.addAttribute("group", group);
        model.addAttribute("mapping", mappingService.getMapping(mid));

        if(!(group.getTarget() == null || group.getClassId() == null)) {
            MyCoReTargetConfiguration targetConfiguration = configuration.getTargets().get(group.getTarget());
            List<MyCoReRestAPIService.Category> categories = myCoReRestAPIService.getClassificationCategories(targetConfiguration, group.getClassId());
            model.addAttribute("categories", categories);
        }

        return "mapping/edit_mapping";
    }

    @RequestMapping(value = "/groups/{gid}/{mid}/edit/", method = RequestMethod.POST)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin', 'mapping-' + #gid)")
    public String editMapping(@PathVariable(name = "gid") Long gid, @PathVariable(name = "mid") Long mid,
        @RequestParam("from") String from,
        @RequestParam("to") String to,
        Model model) {
        MappingGroup group = mappingService.getGroup(gid);
        Mapping mapping = mappingService.getMapping(mid);
        mapping.setFrom(from);
        mapping.setTo(to);
        mappingService.updateMapping(mid, from, to);
        return "redirect:/mapping/groups/" + group.getId() + "/";
    }

    @RequestMapping(value = "/groups/{gid}/{mid}/delete/", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('mapping-admin', 'mapping-' + #gid)")
    public String deleteMapping(@PathVariable(name = "gid") Long gid, @PathVariable(name = "mid") Long mid, Model model) {
        MappingGroup group = mappingService.getGroup(gid);
        mappingService.deleteMapping(mid);
        return "redirect:/mapping/groups/" + group.getId() + "/";
    }
}
