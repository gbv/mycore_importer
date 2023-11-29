package de.vzg.oai_importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import de.vzg.oai_importer.foreign.oai.OAISourceConfiguration;
import de.vzg.oai_importer.foreign.zenodo.ZenodoSourceConfiguration;
import de.vzg.oai_importer.importer.Importer;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.MyCoReSynchronizeService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class JobService {

    @Autowired
    ImporterConfiguration configuration;

    @Autowired
    ApplicationContext context;

    @Autowired
    ImporterService importerService;


    @Autowired
    MyCoReSynchronizeService myCoReSynchronizeService;

    @Autowired
    ForeignEntityRepository repo;

    private HashMap<String, Configuration> getCombinedConfig() {
        Map<String, OAISourceConfiguration> oaiSources =Optional.ofNullable( configuration.getOaiSources())
                .orElseGet(Collections::emptyMap);

        Map<String, ZenodoSourceConfiguration> zenodoSources = Optional.ofNullable(configuration.getZenodoSources())
                .orElseGet(Collections::emptyMap);

        HashMap<String, Configuration> sourceMap = new HashMap<>();

        sourceMap.putAll(oaiSources);
        sourceMap.putAll(zenodoSources);
        return sourceMap;
    }

    public List<ForeignEntity> listImportableRecords(String jobID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);
        Configuration source = getCombinedConfig().get(sourceConfigId);

        return importerService.detectImportableEntities(sourceConfigId, source, target.getUrl());
    }

    public Map<ForeignEntity, List<Mapping>> testMapping(String jobID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);
        Configuration source = getCombinedConfig().get(sourceConfigId);

        List<ForeignEntity> foreignEntities = importerService.detectImportableEntities(sourceConfigId, source, target.getUrl());
        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());

        HashMap<ForeignEntity, List<Mapping>> result = new HashMap<>();

        for (int i = 0; i < foreignEntities.size(); i++) {
            ForeignEntity record = foreignEntities.get(i);
            List<Mapping> missingMappings = importer.checkMapping(target, record);
            result.put(record, missingMappings);
        }

        return result;
    }

    public void runJob(String name) throws OAIException, IOException, URISyntaxException {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(name);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);


        Configuration source =  getCombinedConfig().get(sourceConfigId);

        List<ForeignEntity> records = importerService
            .detectImportableEntities(sourceConfigId, source, target.getUrl());

        String harvesterID = source.getHarvester();
        Harvester<Configuration> harvester = (Harvester<Configuration>) context.getBean(harvesterID);
        harvester.update(sourceConfigId, source);
        myCoReSynchronizeService.synchronize(target);

        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());
        log.info("Found {} records to import", records.size());
        for (int i = 0; i < records.size(); i++) {
            ForeignEntity record = records.get(i);
            importer.importRecord(target, record);
            myCoReSynchronizeService.synchronize(target);
            log.info("{} jobs remaining", records.size() - (i + 1));
        }
    }

    public void import_(String jobID, String recordID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(jobConfig.getTargetConfigId());
        ForeignEntity testRecord = repo.findFirstByConfigIdAndForeignId(sourceConfigId, recordID);

        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());
        importer.importRecord(target, testRecord);
    }

    public String test(String jobID, String recordID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String sourceConfigId = jobConfig.getSourceConfigId();

        ForeignEntity testRecord = repo.findFirstByConfigIdAndForeignId(sourceConfigId, recordID);

        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());
        return importer.testRecord(configuration.getTargets().get(jobConfig.getTargetConfigId()), testRecord);
    }
}
