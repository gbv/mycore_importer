package de.vzg.oai_importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.mycore.MyCoReSynchronizeService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;

@SpringBootApplication()
@EnableConfigurationProperties(ImporterConfiguration.class)
@ComponentScan(excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
    value = de.vzg.oai_importer.OaiImporterCLIApplication.class) })
@AutoConfiguration
@EnableScheduling
public class OaiImporterWebApplication {

    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private ImporterConfiguration configuration;

    @Autowired
    MyCoReSynchronizeService myCoReSynchronizeService;

    @Autowired
    private JobService jobService;

    @Autowired
    ApplicationContext context;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(OaiImporterWebApplication.class, args);
    }

   @Scheduled(cron = "0 30 23 * * *")
    public void test() {
        configuration.getJobs().entrySet().stream()
            .filter(job -> job.getValue().isAuto())
            .map(Map.Entry::getKey)
            .forEach(job -> {
            LOGGER.info("Running job {}", job);
            try {
                ImportJobConfiguration jobConfiguration = configuration.getJobs().get(job);

                // update source (harvest)
                String sourceConfigId = jobConfiguration.getSourceConfigId();
                Configuration sourceCfg = configuration.getCombinedConfig().get(sourceConfigId);
                String harvesterID = sourceCfg.getHarvester();
                Harvester<Configuration> harvester = (Harvester<Configuration>) context.getBean(harvesterID);
                harvester.update(sourceConfigId, sourceCfg, false);

                // update mycore target
                MyCoReTargetConfiguration target = configuration.getTargets().get(jobConfiguration.getTargetConfigId());
                myCoReSynchronizeService.synchronize(target);

                jobService.runJob(job);
            } catch (OAIException | IOException | URISyntaxException e) {
                LOGGER.error("Error while running job {}", job, e);
            }
        });

    }





}
