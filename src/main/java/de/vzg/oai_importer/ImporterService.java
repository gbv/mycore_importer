package de.vzg.oai_importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;

@Service
public class ImporterService {

    @Autowired
    ForeignEntityRepository recordRepository;

    public Page<ForeignEntity> detectImportableEntities(String configID, Configuration source, String targetRepository, Pageable pageable) {
        return recordRepository.findImportableEntities(configID, configID, targetRepository, pageable);
    }

    public Page<Pair<ForeignEntity, MyCoReObjectInfo>> detectUpdateableEntities(String configID, Configuration source, String targetRepository, Pageable pageable) {
        Page<Object[]> updateableEntities = recordRepository.findUpdateableEntities(configID, configID, targetRepository, pageable);
        return updateableEntities.map(objects -> new Pair<>((ForeignEntity) objects[0], (MyCoReObjectInfo) objects[1]));
    }


    public record Pair<T1,T2>(T1 first, T2 second) {
    }

}
