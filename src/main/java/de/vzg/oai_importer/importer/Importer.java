package de.vzg.oai_importer.importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;

import javax.xml.transform.TransformerException;

@Service
public interface Importer {

    boolean importRecord(MyCoReTargetConfiguration target, ForeignEntity record) throws TransformerException, IOException, URISyntaxException;

    boolean updateRecord(MyCoReTargetConfiguration target, ForeignEntity record, MyCoReObjectInfo object);

    String testRecord(MyCoReTargetConfiguration target, ForeignEntity record);

    List<Mapping> checkMapping(MyCoReTargetConfiguration target, ForeignEntity record);

    void setConfig(Map<String, String> importerConfig);


}
