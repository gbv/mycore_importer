package de.vzg.oai_importer;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.importer.PPNLIST2MyCoReImporter;
import de.vzg.oai_importer.mycore.MyCoReSynchronizeService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;

@SpringBootApplication
@EnableConfigurationProperties(ImporterConfiguration.class)
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        value = de.vzg.oai_importer.OaiImporterWebApplication.class)
})
@AutoConfiguration
@ShellComponent
public class OaiImporterCLIApplication {

    private static final Logger LOGGER = LogManager.getLogger();
    @Autowired
    MyCoReSynchronizeService myCoReSynchronizeService;
    @Autowired
    ApplicationContext context;
    @Autowired
    private ImporterConfiguration configuration;
    @Autowired
    private JobService jobService;
    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) throws IOException {
        new SpringApplicationBuilder(OaiImporterCLIApplication.class)
            .web(WebApplicationType.NONE)
            .run(args);
    }

    @ShellMethod(key = "list-jobs", value = "Lists all jobs")
    public void listJobs() {
        LOGGER.info("Available jobs: {}", configuration.getJobs().keySet());
    }

    @ShellMethod(key = "update-source", value = "Updates the source of a job")
    public void updateSource(@ShellOption() String job, @ShellOption(defaultValue = "false") boolean onlyMissing) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Updating source of job {}", job);
        try {
            String sourceConfigId = configuration.getJobs().get(job).getSourceConfigId();
            Configuration source = configuration.getCombinedConfig().get(sourceConfigId);

            Harvester<Configuration> bean
                = (Harvester<Configuration>) applicationContext.getBean(source.getHarvester());
            List<ForeignEntity> updatedRecords = bean.update(sourceConfigId, source, onlyMissing);
            updatedRecords.forEach(record -> LOGGER.info("Updated record {}", record.getForeignId()));
        } catch (RuntimeException e) {
            LOGGER.error("Error while updating source of job {}", job, e.getCause());
        } catch (Exception e) {
            LOGGER.error("Error while updating source of job {}", job, e);
        }

    }

    @ShellMethod(key = "update-target", value = "Updates the target of a job")
    public void updateTarget(@ShellOption() String job) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Updating target of job {}", job);
        try {
            String targetConfigId = configuration.getJobs().get(job).getTargetConfigId();
            MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);

            List<MyCoReObjectInfo> objects = myCoReSynchronizeService.synchronize(target);
            objects.forEach(record -> LOGGER.info("Updated record {}", record.getImportID()));
        } catch (Exception e) {
            LOGGER.error("Error while updating target of job {}", job, e);
        }
    }

    private boolean checkJobPresent(String job) {
        if (!configuration.getJobs().containsKey(job)) {
            LOGGER.error("No job with id {} found", job);
            LOGGER.error("Available jobs: {}", configuration.getJobs().keySet());
            return true;
        }
        return false;
    }

    @ShellMethod(key = "run-importer", value = "Runs the importer")
    public void runImporter(@ShellOption() String job) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Running job {}", job);
        try {
            jobService.runJob(job);
        } catch (Exception e) {
            LOGGER.error("Error while running job {}", job, e);
        }
    }

    @ShellMethod(key = "run-importer-file-check", value = "Runs the importer file check")
    public void runImporterFileCheck(@ShellOption() String job) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Running job {}", job);
        try {

            jobService.runJobFileCheck(job, Pageable.unpaged())
                .stream().filter(record -> !record.getValue().isEmpty())
                .forEach(record -> LOGGER.info("Record {} is missing {} files", record.getKey().getForeignId(),
                    record.getValue().stream().collect(Collectors.joining(", "))));
        } catch (Exception e) {
            LOGGER.error("Error while running job {}", job, e);
        }
    }

    @ShellMethod(key = "run-importer-file-check-for-record", value = "Runs the importer file check for a record")
    public void runImporterFileCheckForRecord(@ShellOption() String job, @ShellOption() String recordId) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Running job {} for record {}", job, recordId);
        try {
            Map.Entry<ForeignEntity, List<String>> foreignEntityListEntry
                = jobService.runJobFileCheckFor(job, recordId);
            if (foreignEntityListEntry != null) {
                LOGGER.info("Record {} is missing {} files", foreignEntityListEntry.getKey().getForeignId(),
                    foreignEntityListEntry.getValue().size());
            } else {
                LOGGER.info("Record {} not found", recordId);
            }
        } catch (Exception e) {
            LOGGER.error("Error while running job {}", job, e);
        }
    }

    @ShellMethod(key = "run-importer-file-import", value = "Runs the importer which imports files to existing records")
    public void runImporterFileImport(@ShellOption() String job) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Running job {}", job);
        try {
            jobService.runJobFileImport(job, Pageable.unpaged()).stream().filter(record -> !record.getValue().isEmpty())
                .forEach(record -> {
                    LOGGER.info("Added missing files to {}:{}", record.getKey().getForeignId(),
                        record.getValue().stream().collect(Collectors.joining(", ")));
                });
        } catch (Exception e) {
            LOGGER.error("Error while running job {}", job, e);
        }
    }

    @ShellMethod(key = "check-never-should-have-been-imported",
        value = "Checks if records have been imported that should not have been imported")
    public void checkNeverShouldHaveBeenImported(@ShellOption() String job) {
        if (checkJobPresent(job)) {
            return;
        }
        ImportJobConfiguration importJobConfiguration = jobService.configuration.getJobs().get(job);
        PPNLIST2MyCoReImporter importer
            = context.getBean(importJobConfiguration.getImporter(), PPNLIST2MyCoReImporter.class);
        importer.setConfig(importJobConfiguration.getImporterConfig());

        LOGGER.info("Running job {}", job);
        Pageable pageable = Pageable.ofSize(10000);
        Page<ImporterService.Pair<ForeignEntity, MyCoReObjectInfo>> pairs;
        try {
            do {
                Date date = new Date();
                pairs = jobService.listUpdateableRecords(job, pageable);
                LOGGER.info("Retrieved {} records in {}", pairs.getNumberOfElements(), new Date().getTime() - date.getTime());
                pairs.stream().filter(pair -> {
                    try {
                        return importer.shouldNotBeImported(pair.first(), pair.second());
                    } catch (IOException | JDOMException e) {
                        return true;
                    }
                }).forEach(pair -> {
                    LOGGER.info("Record {} with id {} should not have been imported", pair.first().getForeignId(),
                        pair.second().getMycoreId());
                });
                pageable = pageable.next();
            } while(pairs.hasNext());
        } catch (Exception e) {
            LOGGER.error("Error while running job {}", job, e);
        }
    }

    @ShellMethod(key = "run-update", value = "Runs the update")
    public void runUpdate(@ShellOption() String job) {
        if (checkJobPresent(job)) {
            return;
        }

        LOGGER.info("Running update job {}", job);
        try {
            jobService.runUpdateJob(job);
        } catch (Exception e) {
            LOGGER.error("Error while running update job {}", job, e);
        }
    }
}
