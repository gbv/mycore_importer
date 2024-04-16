package de.vzg.oai_importer.importer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.mycore.pica2mods.xsl.Pica2ModsManager;
import org.mycore.pica2mods.xsl.Pica2ModsXSLTURIResolver;
import org.mycore.pica2mods.xsl.model.Pica2ModsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.ImporterService;
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

    Namespace picaxml = Namespace.getNamespace("info:srw/schema/5/picaXML-v1.0");

    private static Document getForeignEntityDocument(ForeignEntity record) {
        Document doc1;
        try (var sr = new StringReader(record.getMetadata())) {
            doc1 = new SAXBuilder().build(sr);
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }
        return doc1;
    }

    private static String extractFileName(String realURL) {
        String fileName = realURL.substring(realURL.lastIndexOf('/') + 1).trim();
        fileName = fileName.replaceAll("[^A-Za-z0-9_\\-.]", "");
        return fileName;
    }

    @SneakyThrows
    @Override
    public boolean importRecord(MyCoReTargetConfiguration target, ForeignEntity record) {
        // Convert to Mods
        var mods = convertToMods(target, record);
        var object = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));
        MODSUtil.setRecordInfo(object, record.getForeignId(), record.getConfigId());

        // check files
        var davPath = config.get("file-path");
        var path = Paths.get(davPath);

        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        var filePPN = Stream.of(getPicaField(picaXML, "003@0", "0"), getPicaField(picaXML, "007G", "0"))
            .flatMap(s -> s)
            .distinct()
            .collect(Collectors.toList());

        List<Path> files = resolveFiles(record, filePPN, path);

        log.info("Found {} files for record {} in filesystem with ppn {}", files.size(), record.getForeignId(),
            filePPN);

        // transfer everything
        var location = restAPIService.postObject(target, object);
        var mycoreID = location.substring(location.lastIndexOf("/") + 1);

        List<String> fileURLs = getPicaField(picaXML, "017C", "u")
                .toList();

        List<String> filteredFileURLs = fileURLs.stream()
                .filter(url -> (url.startsWith("http") || url.startsWith("https")) && (url.toLowerCase(Locale.ROOT).endsWith(".pdf") || url.contains("//www.dfi.de/")))
                .toList();

        // transfer files
        if (!files.isEmpty() || !filteredFileURLs.isEmpty()) {
            List<ImporterService.Pair<String, byte[]>> downloadedFiles = new ArrayList<>();
            if (files.isEmpty()) {
                filteredFileURLs.stream()
                    .map(url -> {
                        try {
                            return downloadFile(url, 0);
                        } catch (IOException e) {
                            log.error("Error while downloading file {} for record {}", url, record.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .peek(p -> log.info("Downloaded file {} for record {}", p.first(), record.getId()))
                    .forEach(downloadedFiles::add);
            }

            if (files.isEmpty() && downloadedFiles.isEmpty()) {
                log.error("No files found for record {}", record.getId());
                return true;
            }

            log.info("Create derivate  for record {}", record.getId());
            String maindoc = files.isEmpty() ? downloadedFiles.stream().map(ImporterService.Pair::first)
                .map(PPNLIST2MyCoReImporter::extractFileName).findFirst().get()
                : files.get(0).getFileName().toString();

            List<String> classifications;
            if (fileURLs.isEmpty()) {
                classifications = List.of("derivate_types:content", "mir_access:ipAddressRange");
            } else {
                classifications = List.of("derivate_types:content");
            }

            String derivateURL = restAPIService.postDerivate(target, mycoreID, "0", maindoc, classifications,
                Collections.emptyList());
            log.info("Created derivate {} for record {}", derivateURL, record.getId());
            String derivateID = derivateURL.substring(derivateURL.lastIndexOf("/") + 1);

            // prefer files over URL
            if (!files.isEmpty()) {
                for (Path p : files) {
                    log.info("Import file {} to {}", p.getFileName(), mycoreID);
                    restAPIService.putFiles(target, mycoreID, derivateID, p.getFileName().toString(),
                        Files.newInputStream(p));
                    log.info("Imported file {} to {}", p.getFileName(), mycoreID);
                }
            } else {
                for (var fileDownload : downloadedFiles) {
                    try (InputStream is = new ByteArrayInputStream(fileDownload.second())) {
                        String realURL = fileDownload.first();
                        log.info("Import file {} to {}", realURL, mycoreID);
                        restAPIService.putFiles(target, mycoreID, derivateID,
                            extractFileName(realURL),
                            is);
                        log.info("Imported file {} to {}", realURL, mycoreID);
                    }
                }
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

    private static List<Path> resolveFiles(ForeignEntity record, List<String> filePPN, Path path) {
        List<Path> files = new ArrayList<>();
        for (String s : filePPN) {
            String substring = s.substring(0, 2);
            Path resolve = path.resolve(substring);
            if (Files.exists(resolve)) {
                try (var list = Files.list(resolve)) {
                    list.filter(p -> p.getFileName().toString().startsWith(s + "-"))
                        .forEach(files::add);
                } catch (IOException e) {
                    log.error("Error while listing files for record {}", record.getId(), e);
                }
            }
        }
        return files;
    }

    public ImporterService.Pair<String, byte[]> downloadFile(String url, int redirectCount) throws IOException {
        if(redirectCount > 5) {
            throw new IOException("Too many redirects");
        }
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(4000);
        con.connect();
        if (con.getResponseCode() == 301 || con.getResponseCode() == 302 || con.getResponseCode() == 307) {
            String real = con.getHeaderField("Location");
            try (InputStream inputStream = con.getInputStream()) {
                inputStream.readAllBytes();
            }
            con.disconnect();

            if (real.startsWith("/") && url.contains("//")) {
                real = url.substring(0, url.indexOf('/', url.indexOf("//") + 2)) + real.substring(1);
            }
            return downloadFile(real, redirectCount+1);
        } else {
            try (InputStream is = con.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                return new ImporterService.Pair<>(url, bytes);
            } finally {
                con.disconnect();
            }
        }
    }

    Stream<String> getPicaField(Document root, String tag, String code) {
        return root.getRootElement().getChildren("datafield", picaxml)
            .stream()
            .filter(e -> e.getAttributeValue("tag").equals(tag))
            .map(element -> element.getChildren().stream().filter(e -> e.getAttributeValue("code").equals(code))
                .findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Element::getText);
    }

    @SneakyThrows
    @Override
    public String testRecord(MyCoReTargetConfiguration target, ForeignEntity record) {

        String mods = convertToMods(target, record);
        Document document = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));

        MODSUtil.setRecordInfo(document, record.getForeignId(), record.getConfigId());

        return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
    }

    @SneakyThrows
    @Override
    public boolean updateRecord(MyCoReTargetConfiguration target, ForeignEntity record, MyCoReObjectInfo objectInfo) {
        var mods = convertToMods(target, record);
        var object = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));
        MODSUtil.setRecordInfo(object, record.getForeignId(), record.getConfigId());

        Document metadata = new Document(MODSUtil.getMetadata(object).detach());
        restAPIService.putObjectMetadata(target, objectInfo.getMycoreId(), metadata);

        return true;
    }

    private String convertToMods(MyCoReTargetConfiguration target, ForeignEntity record) throws TransformerException {
        String resultStr;
        Document doc1 = getForeignEntityDocument(record);

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

        String stylesheetName = config.get("stylesheet");
        Source xsl = new StreamSource(var10002.getResourceAsStream(stylesheetName));
        xsl.setSystemId(stylesheetName);
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
        return Collections.emptyList();
    }

    @Override
    public void setConfig(Map<String, String> importerConfig) {
        this.config = importerConfig;
    }
}
