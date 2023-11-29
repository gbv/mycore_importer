package de.vzg.oai_importer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;

@Service
public class ImporterService {

    @Autowired
    ForeignEntityRepository recordRepository;

    public List<ForeignEntity> detectImportableEntities(String configID, Configuration source, String targetRepository) {
        return recordRepository.findImportableEntities(configID, configID, targetRepository);
    }



}
