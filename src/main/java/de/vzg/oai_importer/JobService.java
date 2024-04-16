package de.vzg.oai_importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import de.vzg.oai_importer.importer.Importer;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.MyCoReSynchronizeService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfoRepository;
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
    MyCoReObjectInfoRepository mycoreRepo;

    @Autowired
    ForeignEntityRepository repo;


    public Page<ForeignEntity> listImportableRecords(String jobID, Pageable pageable) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);
        Configuration source = configuration.getCombinedConfig().get(sourceConfigId);

        return importerService.detectImportableEntities(sourceConfigId, source, target.getUrl(), pageable);
    }

    public Page<ImporterService.Pair<ForeignEntity, MyCoReObjectInfo>> listUpdateableRecords(String jobID,
        Pageable pageable) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);
        Configuration source = configuration.getCombinedConfig().get(sourceConfigId);

        return importerService.detectUpdateableEntities(sourceConfigId, source, target.getUrl(), pageable);
    }

    public Map<ForeignEntity, List<Mapping>> testMapping(String jobID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);
        Configuration source = configuration.getCombinedConfig().get(sourceConfigId);

        Page<ForeignEntity> foreignEntities = importerService.detectImportableEntities(sourceConfigId, source, target.getUrl(), Pageable.unpaged());
        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());

        HashMap<ForeignEntity, List<Mapping>> result = new HashMap<>();

        foreignEntities.forEach(record -> {
            List<Mapping> missingMappings = importer.checkMapping(target, record);
            result.put(record, missingMappings);
        });

        return result;
    }

    public void runJob(String name) throws OAIException, IOException, URISyntaxException {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(name);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);


        Configuration source =  configuration.getCombinedConfig().get(sourceConfigId);

        Page<ForeignEntity> records = importerService
            .detectImportableEntities(sourceConfigId, source, target.getUrl(), Pageable.unpaged());

        /*
        String harvesterID = source.getHarvester();
        Harvester<Configuration> harvester = (Harvester<Configuration>) context.getBean(harvesterID);
        harvester.update(sourceConfigId, source);
        myCoReSynchronizeService.synchronize(target);
         */

        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());
        log.info("Found {} records to import", records.getTotalElements());

        AtomicLong i = new AtomicLong(records.getTotalElements());
        records.forEach(record -> {
            try {
                importer.importRecord(target, record);
                try {
                    myCoReSynchronizeService.synchronize(target);
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                log.info("{} jobs remaining", i.decrementAndGet());
            } catch (Exception e) {
                log.error("Error while importing record {}", record.getForeignId(), e);
            }
        });
    }

    public void runUpdateJob(String name) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(name);
        String targetConfigId = jobConfig.getTargetConfigId();
        String sourceConfigId = jobConfig.getSourceConfigId();
        MyCoReTargetConfiguration target = configuration.getTargets().get(targetConfigId);
        Configuration source = configuration.getCombinedConfig().get(sourceConfigId);

        var updatableEntities
            = importerService.detectUpdateableEntities(sourceConfigId, source, target.getUrl(), Pageable.unpaged());

        List<String> errorRecords = new ArrayList<>();

        var count = 0;
        for (var pair : updatableEntities) {
            log.info("Updating record {}/{}", count++, updatableEntities.getTotalElements());
            Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
            importer.setConfig(jobConfig.getImporterConfig());
            try {
                importer.updateRecord(target, pair.first(), pair.second());
                // catch springs application stopped exception
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error while updating record {}", pair.first().getForeignId(), e);
                errorRecords.add(pair.first().getForeignId());
            }
        }

        log.info("Records with errors: {}", errorRecords);
    }

    public void importSingleDocument(String jobID, String recordID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(jobConfig.getTargetConfigId());
        ForeignEntity testRecord = repo.findFirstByConfigIdAndForeignId(sourceConfigId, recordID);

        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());
        importer.importRecord(target, testRecord);
    }

    public void updateSingleDocument(String jobID, String recordID) {
        ImportJobConfiguration jobConfig = configuration.getJobs().get(jobID);
        String sourceConfigId = jobConfig.getSourceConfigId();

        MyCoReTargetConfiguration target = configuration.getTargets().get(jobConfig.getTargetConfigId());
        ForeignEntity testRecord = repo.findFirstByConfigIdAndForeignId(sourceConfigId, recordID);

        MyCoReObjectInfo object = mycoreRepo.findFirstByRepositoryAndImportURLAndImportID(target.getUrl(),
            jobConfig.getSourceConfigId(), testRecord.getForeignId());

        Importer importer = context.getBean(jobConfig.getImporter(), Importer.class);
        importer.setConfig(jobConfig.getImporterConfig());
        importer.updateRecord(target, testRecord, object);
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
