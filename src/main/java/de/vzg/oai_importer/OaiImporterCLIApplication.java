package de.vzg.oai_importer;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
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
