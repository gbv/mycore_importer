package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.vzg.oai_importer.ImporterConfiguration;
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
    public String showJob(@PathVariable("jobID") String jobID, Model model) {
        List<ForeignEntity> records = jobService.listImportableRecords(jobID);
        model.addAttribute("records", records);
        model.addAttribute("jobID", jobID);
        return "job_records";
    }

    @RequestMapping("/{jobID}/update/")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String showUpdateJob(@PathVariable("jobID") String jobID, Model model) {
        Map<ForeignEntity, MyCoReObjectInfo> records = jobService.listUpdateableRecords(jobID);
        model.addAttribute("records", records);

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
    public String runJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID) {
        jobService.importSingleDocument(jobID, recordID);

        return "redirect:/jobs/" + jobID + "/update/";
    }

    @RequestMapping("/{jobID}/update/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String updateJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID) {
        jobService.updateSingleDocument(jobID, recordID);

        return "redirect:/jobs/" + jobID + "/update/";
    }

    @RequestMapping("/{jobID}/import")
    public String runJob(@PathVariable("jobID") String jobID, Model model) throws IOException, URISyntaxException, OAIException {
        model.addAttribute("jobID", jobID);
        jobService.runJob(jobID);
        return "job_test";
    }

}
