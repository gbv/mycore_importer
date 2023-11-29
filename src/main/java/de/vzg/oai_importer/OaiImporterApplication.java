package de.vzg.oai_importer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ImporterConfiguration.class)
@AutoConfiguration
@EnableScheduling
public class OaiImporterApplication {

    private static final Logger LOGGER = LogManager.getLogger();
    @Autowired
    private ImporterConfiguration configuration;
    @Autowired
    private JobService jobService;

    public static void main(String[] args) {
        SpringApplication.run(OaiImporterApplication.class, args);
    }

    /*
   @Scheduled(fixedDelay = 300000, initialDelay = 3000)
    public void test() {
        configuration.getJobs().keySet().forEach(job -> {
            LOGGER.info("Running job {}", job);
            try {
                jobService.runJob(job);
            } catch (OAIException | IOException | URISyntaxException e) {
                LOGGER.error("Error while running job {}", job, e);
            }
        });

    }


     */


}
