package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.ImporterService;
import de.vzg.oai_importer.JobService;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;


@Controller
@RequestMapping("/jobs")
@PreAuthorize("hasAnyAuthority('job')")
public class JobsController {

    @Autowired
    private ImporterConfiguration configuration;

    @Autowired
    private JobService jobService;

    @RequestMapping("/")
    public String listJobs(Model model) {
        model.addAttribute("jobs", configuration.getJobs());
        return "jobs_list_config";
    }

    @RequestMapping("/{jobID}/")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String showJob(@PathVariable("jobID") String jobID,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "100") int size,
                          Model model) {
        Page<ForeignEntity> records = jobService.listImportableRecords(jobID, Pageable.ofSize(size).withPage(page));
        model.addAttribute("records", records);
        model.addAttribute("jobID", jobID);
        model.addAttribute("pages", IntStream.rangeClosed(1, records.getTotalPages())
                .boxed());
        return "job_records";
    }

    @RequestMapping("/{jobID}/update/")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String showUpdateJob(@PathVariable("jobID") String jobID,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "100") int size,
                                Model model,
                                @RequestParam(value = "success", required = false) String success) {
        Page<ImporterService.Pair<ForeignEntity, MyCoReObjectInfo>> records = jobService.listUpdateableRecords(jobID, Pageable.ofSize(size).withPage(page));
        model.addAttribute("records", records);
        model.addAttribute("jobID", jobID);
        model.addAttribute("pages", IntStream.rangeClosed(1, records.getTotalPages())
                .boxed());
        if(success != null && (success.equals("true") || success.equals("false"))) {
            model.addAttribute("success", success);
        }

        return "job_update";
    }


    @RequestMapping("/{jobID}/testMapping")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runTestJob(@PathVariable("jobID") String jobID, Model model) {
        Map<ForeignEntity, List<Mapping>> records = jobService.testMapping(jobID);

        model.addAttribute("records", records);

        return "job_mapping_test";
    }

    @RequestMapping("/{jobID}/test/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runTestJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID, Model model) {
        model.addAttribute("jobID", jobID);
        model.addAttribute("recordID", recordID);
        model.addAttribute("result", jobService.test(jobID, recordID));

        return "job_test";
    }

    @RequestMapping("/{jobID}/import/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID,
                          RedirectAttributes redirectAttributes) {
        jobService.importSingleDocument(jobID, recordID);

        redirectAttributes.addAttribute("success", "true");

        return "redirect:/jobs/" + jobID + "/update/";
    }

    @RequestMapping("/{jobID}/update/update")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String updateJob(@PathVariable("jobID") String jobID,
                            RedirectAttributes redirectAttributes) {
        jobService.runUpdateJob(jobID);
        redirectAttributes.addAttribute("success", "true");

        return "redirect:/jobs/" + jobID + "/update/";
    }

    @RequestMapping("/{jobID}/update/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String updateJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID,
                            RedirectAttributes redirectAttributes) {
        jobService.updateSingleDocument(jobID, recordID);

        redirectAttributes.addAttribute("success", "true");

        return "redirect:/jobs/" + jobID + "/update/";
    }

    @RequestMapping("/{jobID}/import")
    public String runJob(@PathVariable("jobID") String jobID, Model model) throws IOException, URISyntaxException, OAIException {
        model.addAttribute("jobID", jobID);
        jobService.runJob(jobID);
        return "job_test";
    }

}
