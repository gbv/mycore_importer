package de.vzg.oai_importer.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.mycore.pica2mods.xsl.Pica2ModsManager;
import org.mycore.pica2mods.xsl.Pica2ModsXSLTURIResolver;
import org.mycore.pica2mods.xsl.model.Pica2ModsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.MODSUtil;
import de.vzg.oai_importer.mycore.MyCoReRestAPIService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service("PPNLIST2MyCoReImporter")
@Log4j2
public class PPNLIST2MyCoReImporter implements Importer {
    @Autowired
    MyCoReRestAPIService restAPIService;
    private Map<String, String> config;

    @SneakyThrows
    @Override
    public boolean importRecord(MyCoReTargetConfiguration target, ForeignEntity record) {
        // Convert to Mods
        var mods = convertToMods(target, record);
        var object = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));
        MODSUtil.setRecordInfo(object, record.getForeignId(), record.getConfigId());

        // check files
        var davPath = config.get("dav-path");
        var path = Paths.get(davPath);

        var prefix = record.getForeignId().substring(0, 2);
        var prefixPath = path.resolve(prefix);

        if (!Files.exists(prefixPath)) {
            return false;
        }

        List<Path> files = null;
        try (var list = Files.list(prefixPath)) {
            files = list.filter(p -> p.getFileName().toString().startsWith(record.getForeignId()))
                .toList();
        }
        log.info("Found {} files for record {}", files.size(), record.getForeignId());

        // transfer everything
        var location = restAPIService.postObject(target, object);
        var mycoreID = location.substring(location.lastIndexOf("/") + 1);

        // transfer files
        if (!files.isEmpty()) {
            log.info("Certe derivate  for record {}", record.getId());
            String derivateURL = restAPIService.postDerivate(target,
                mycoreID,
                "0",
                files.get(0).getFileName().toString(),
                List.of("derivate_types:content", "mir_access:ipAddressRange"),
                Collections.emptyList());
            log.info("Created derivate {} for record {}", derivateURL, record.getId());
            String derivateID = derivateURL.substring(derivateURL.lastIndexOf("/") + 1);

            for (Path p : files) {
                log.info("Import file {} to {}", p.getFileName(), mycoreID);
                restAPIService.putFiles(target, mycoreID, derivateID, p.getFileName().toString(),
                    Files.newInputStream(p));
                log.info("Imported file {} to {}", p.getFileName(), mycoreID);
            }
            /*
            Document createdDerivate = restAPIService.getDerivate(target, mycoreID, derivateID);
            MyCoReUtil.setMainFile(createdDerivate, files.get(0).getFileName().toString());
            restAPIService.putDerivate(target, mycoreID, derivateID, createdDerivate);

             */
        }

        log.info("Imported record {} to {}", record.getId(), mycoreID);

        return true;
    }

    @Override
    public boolean updateRecord(MyCoReTargetConfiguration target, ForeignEntity record, MyCoReObjectInfo object) {
        return false;
    }

    @SneakyThrows
    @Override
    public String testRecord(MyCoReTargetConfiguration target, ForeignEntity record) {

        String mods = convertToMods(target, record);
        Document document = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));

        MODSUtil.setRecordInfo(document, record.getForeignId(), record.getConfigId());

        return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
    }

    private String convertToMods(MyCoReTargetConfiguration target, ForeignEntity record) throws TransformerException {
        String resultStr;
        Document doc1;
        try (var sr = new StringReader(record.getMetadata())) {
            doc1 = new SAXBuilder().build(sr);
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }

        Pica2ModsConfig pica2ModsConfig = new Pica2ModsConfig();
        pica2ModsConfig.setUnapiUrl("https://unapi.k10plus.de/");
        pica2ModsConfig.setMycoreUrl(target.getUrl());
        pica2ModsConfig.setCatalogs(new HashMap<>());

        Pica2ModsManager pica2ModsManager = new Pica2ModsManager(pica2ModsConfig);

        TransformerFactory TRANS_FACTORY
            = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", this.getClass().getClassLoader());
        TRANS_FACTORY.setURIResolver(new Pica2ModsXSLTURIResolver(pica2ModsManager));
        TRANS_FACTORY.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

        ClassLoader var10002 = this.getClass().getClassLoader();

        Source xsl = new StreamSource(var10002.getResourceAsStream("xsl/pica2mods.xsl"));
        xsl.setSystemId("xsl/pica2mods.xsl");
        Transformer transformer = TRANS_FACTORY.newTransformer(xsl);
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.setParameter("WebApplicationBaseURL", pica2ModsConfig.getMycoreUrl());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        transformer.transform(new JDOMSource(doc1), result);
        resultStr = baos.toString(StandardCharsets.UTF_8);
        return resultStr;
    }

    @Override
    public List<Mapping> checkMapping(MyCoReTargetConfiguration target, ForeignEntity record) {
        return null;
    }

    @Override
    public void setConfig(Map<String, String> importerConfig) {
        this.config = importerConfig;
    }
}
