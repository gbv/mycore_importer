package de.vzg.oai_importer.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import de.vzg.oai_importer.AuthorityFilter;
import de.vzg.oai_importer.ImporterConfiguration;
import de.vzg.oai_importer.ImporterService;
import de.vzg.oai_importer.JobResult;
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
    public String listJobs(Model model, Authentication authentication) {
        model.addAttribute("jobs", AuthorityFilter.filter(configuration.getJobs(), "job", authentication));
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
        return "job_records";
    }

    @RequestMapping("/{jobID}/fileCheck")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runFileCheckJob(@PathVariable("jobID") String jobID,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size, Model model) {

        Page<Map.Entry<ForeignEntity, List<String>>> records =
            jobService.listImportableFiles(jobID, Pageable.ofSize(size).withPage(page));
        model.addAttribute("records", records);
        model.addAttribute("jobID", jobID);

        return "job_records_file_check";
    }

    @RequestMapping("/{jobID}/update/fileCheck")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runFileCheckUpdateJob(@PathVariable("jobID") String jobID,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size, Model model) {
        Page<Map.Entry<ForeignEntity, List<String>>> records =
            jobService.runJobFileCheck(jobID, Pageable.ofSize(size).withPage(page));
        model.addAttribute("records", records);
        model.addAttribute("jobID", jobID);

        return "job_records_file_check_update";
    }

    @RequestMapping("/{jobID}/update/")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String showUpdateJob(@PathVariable("jobID") String jobID,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size,
        Model model) {
        Page<ImporterService.Pair<ForeignEntity, MyCoReObjectInfo>> records =
            jobService.listUpdateableRecords(jobID, Pageable.ofSize(size).withPage(page));
        model.addAttribute("records", records);
        model.addAttribute("jobID", jobID);

        return "job_update";
    }

    @RequestMapping("/{jobID}/testMapping")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runTestJob(@PathVariable("jobID") String jobID, Model model,
        @RequestParam(value = "update", required = false, defaultValue = "false") boolean update) {
        Map<ForeignEntity, List<Mapping>> records = jobService.testMapping(jobID, update);

        model.addAttribute("records", records);

        return "job_mapping_test";
    }

    @RequestMapping("/{jobID}/test/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runTestJob(@PathVariable("jobID") String jobID,
        @PathVariable("recordID") String recordID, Model model) {
        model.addAttribute("jobID", jobID);
        model.addAttribute("recordID", recordID);
        model.addAttribute("result", jobService.test(jobID, recordID));

        return "job_test";
    }

    @RequestMapping("/{jobID}/import/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID,
        RedirectAttributes redirectAttributes) {
        return showResult(jobID, jobService.importDocuments(jobID, List.of(recordID)), redirectAttributes);
    }

    @PostMapping("/{jobID}/importSelected")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String importSelected(@PathVariable("jobID") String jobID,
        @RequestParam(value = "recordID", required = false) List<String> recordIDs,
        RedirectAttributes redirectAttributes) {
        if (recordIDs == null || recordIDs.isEmpty()) {
            return "redirect:/jobs/" + jobID + "/";
        }

        return showResult(jobID, jobService.importDocuments(jobID, recordIDs), redirectAttributes);
    }

    @RequestMapping("/{jobID}/update/update")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String updateJob(@PathVariable("jobID") String jobID,
        RedirectAttributes redirectAttributes) {
        return showResult(jobID, jobService.runUpdateJob(jobID), redirectAttributes);
    }

    @RequestMapping("/{jobID}/update/{recordID}")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String updateJob(@PathVariable("jobID") String jobID, @PathVariable("recordID") String recordID,
        RedirectAttributes redirectAttributes) {
        return showResult(jobID, jobService.updateDocuments(jobID, List.of(recordID)), redirectAttributes);
    }

    @RequestMapping("/{jobID}/import")
    @PreAuthorize("hasAnyAuthority('job-' + #jobID)")
    public String runJob(@PathVariable("jobID") String jobID, RedirectAttributes redirectAttributes)
        throws IOException, URISyntaxException, OAIException {
        return showResult(jobID, jobService.runJob(jobID), redirectAttributes);
    }

    /**
     * Redirects to the imported documents of the job, which then report the outcome of the job.
     *
     * @param jobID the job that was run
     * @param result the outcome of the job
     * @param redirectAttributes carries the outcome over the redirect
     * @return the redirect to the imported documents of the job
     */
    private String showResult(String jobID, JobResult result, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("result", result);
        return "redirect:/jobs/" + jobID + "/update/";
    }

}
