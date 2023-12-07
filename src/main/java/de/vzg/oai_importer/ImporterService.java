package de.vzg.oai_importer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.Configuration;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;

@Service
public class ImporterService {

    @Autowired
    ForeignEntityRepository recordRepository;

    public List<ForeignEntity> detectImportableEntities(String configID, Configuration source, String targetRepository) {
        return recordRepository.findImportableEntities(configID, configID, targetRepository);
    }

    public Map<ForeignEntity, MyCoReObjectInfo> detectUpdateableEntities(String configID, Configuration source, String targetRepository) {
        List<Object[]> updateableEntities = recordRepository.findUpdateableEntities(configID, configID, targetRepository);

        LinkedHashMap<ForeignEntity, MyCoReObjectInfo> result = new LinkedHashMap<>(updateableEntities.size());
        for (Object[] objects : updateableEntities) {
            ForeignEntity foreignEntity = (ForeignEntity) objects[0];
            MyCoReObjectInfo myCoReObjectInfo = (MyCoReObjectInfo) objects[1];
            result.put(foreignEntity, myCoReObjectInfo);
        }

        return result;
    }


}
