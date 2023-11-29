package de.vzg.oai_importer.importer;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;

@Service
public interface Importer {

    boolean importRecord(MyCoReTargetConfiguration target, ForeignEntity record);

    String testRecord(MyCoReTargetConfiguration target, ForeignEntity record);

    List<Mapping> checkMapping(MyCoReTargetConfiguration target, ForeignEntity record);

    void setConfig(Map<String, String> importerConfig);


}
