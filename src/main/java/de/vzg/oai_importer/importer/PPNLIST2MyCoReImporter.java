package de.vzg.oai_importer.importer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.ImporterService;
import de.vzg.oai_importer.PicaUtils;
import de.vzg.oai_importer.dfi.DFISRURecordFilter;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mycore.MODSUtil;
import de.vzg.oai_importer.mycore.MyCoReRestAPIService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.MyCoReUtil;
import de.vzg.oai_importer.mycore.api.model.MyCoReFileListDirectory;
import de.vzg.oai_importer.mycore.api.model.MyCoReFileListFile;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service("PPNLIST2MyCoReImporter")
@Log4j2
public class PPNLIST2MyCoReImporter implements Importer, FileBased {

    @Autowired
    MyCoReRestAPIService restAPIService;

    @Autowired
    ApplicationContext context;

    Namespace picaxml = Namespace.getNamespace("info:srw/schema/5/picaXML-v1.0");

    private Map<String, String> config;

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

    private static List<Path> resolveFiles(ForeignEntity record, List<String> filePPN, Path path, Path newPath) {
        List<Path> files = new ArrayList<>();
        for (String s : filePPN) {
            String substring = s.substring(0, 2);
            Path resolve = path.resolve(substring);
            // check if the file is in the old path
            if (Files.exists(resolve)) {
                try (var list = Files.list(resolve)) {
                    list.filter(p -> p.getFileName().toString().startsWith(s + "-"))
                        .forEach(files::add);
                } catch (IOException e) {
                    log.error("Error while listing files for record {}", record.getId(), e);
                }
            }
            // in the new file path we check for the direct file name, because there is no subfolder
            try (var list = Files.list(newPath)) {
                list.filter(p -> {
                    String fileNameStr = p.getFileName().toString();
                    return fileNameStr.startsWith(s + "-") || fileNameStr.startsWith(s + " -");
                }).forEach(files::add);
            } catch (IOException e) {
                log.error("Error while listing new files for record {}", record.getId(), e);
            }
        }
        return files;
    }

    public boolean isPublic(String ppn, Element record) {
        String fileRightsDetector = this.config.get("file-rights-detector-service");
        if (fileRightsDetector == null) {
            return true;
        }
        boolean aPublic = this.context.getBean(fileRightsDetector, FileRightsDetector.class).isPublic(record);
        log.info("The record {} is {}", ppn, aPublic ? "public" : "not public");

        return aPublic;
    }


    @Override
    public boolean importRecord(MyCoReTargetConfiguration target, ForeignEntity record) throws TransformerException, IOException, URISyntaxException {
        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        // Convert to Mods
        List<String> fileURLs = PicaUtils.getPicaField(picaXML, "017C", "u")
            .toList();
        boolean free = !fileURLs.isEmpty() && isPublic(record.getForeignId(), picaXML.getRootElement());

        var mods = convertToMods(target, record, free, picaXML);
        var object = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));
        MODSUtil.setRecordInfo(object, record.getForeignId(), record.getConfigId());
        MODSUtil.sortMODSInMyCoreObject(object);

        // check files
        var davPath = config.get("file-path");
        var newFilesPath = config.get("new-file-path");

        List<String> filePPN;
        List<Path> files;

        if (davPath != null && newFilesPath != null) {
            var path = Paths.get(davPath);
            var newPath = Paths.get(newFilesPath);

            filePPN = Stream.of(PicaUtils.getPicaField(picaXML, "003@", "0"),
                            PicaUtils.getPicaField(picaXML, "007G", "0"))
                    .flatMap(s -> s)
                    .distinct()
                    .collect(Collectors.toList());

            files = resolveFiles(record, filePPN, path, newPath);
        } else {
            filePPN = Collections.emptyList();
            files = Collections.emptyList();
        }



        log.info("Found {} files for record {} in filesystem with ppn {}", files.size(), record.getForeignId(),
            filePPN);

        List<String> filteredFileURLs = fileURLs.stream()
            .filter(url -> (url.startsWith("http") || url.startsWith("https")) &&
                (url.toLowerCase(Locale.ROOT).endsWith(".pdf") || url.contains("//www.dfi.de/"))
            && !url.contains("bnpparibas"))
                .toList();

        // transfer everything
        var location = restAPIService.postObject(target, object);
        var mycoreID = location.substring(location.lastIndexOf("/") + 1);

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

            String derivateID = createDerivate(target, record, free, mycoreID, maindoc);

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

    private String createDerivate(MyCoReTargetConfiguration target, ForeignEntity record, boolean free, String mycoreID,
        String maindoc) throws IOException, URISyntaxException {
        List<String> classifications;
        if (!free) {
            classifications = List.of("derivate_types:content", "mir_access:ipAddressRange");
        } else {
            classifications = List.of("derivate_types:content");
        }

        String derivateURL = restAPIService.postDerivate(target, mycoreID, "0", maindoc, classifications,
            Collections.emptyList());
        log.info("Created derivate {} for record {}", derivateURL, record.getId());
        String derivateID = derivateURL.substring(derivateURL.lastIndexOf("/") + 1);
        return derivateID;
    }

    public ImporterService.Pair<String, byte[]> downloadFile(String url, int redirectCount) throws IOException {
        if (redirectCount > 5) {
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
            return downloadFile(real, redirectCount + 1);
        } else {
            try (InputStream is = con.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                return new ImporterService.Pair<>(url, bytes);
            } finally {
                con.disconnect();
            }
        }
    }

    @SneakyThrows
    @Override
    public String testRecord(MyCoReTargetConfiguration target, ForeignEntity record) {
        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        // Convert to Mods
        List<String> fileURLs = PicaUtils.getPicaField(picaXML, "017C", "u")
            .toList();
        boolean free = !fileURLs.isEmpty() && isPublic(record.getForeignId(), picaXML.getRootElement());
        String mods = convertToMods(target, record, free, picaXML);
        Document document = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));

        MODSUtil.setRecordInfo(document, record.getForeignId(), record.getConfigId());
        MODSUtil.sortMODSInMyCoreObject(document);
        return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
    }

    @SneakyThrows
    @Override
    public boolean updateRecord(MyCoReTargetConfiguration target, ForeignEntity record, MyCoReObjectInfo objectInfo) {
        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        // Convert to Mods
        List<String> fileURLs = PicaUtils.getPicaField(picaXML, "017C", "u")
            .toList();
        boolean free = !fileURLs.isEmpty() && isPublic(record.getForeignId(), picaXML.getRootElement());
        var mods = convertToMods(target, record, free, picaXML);
        var object = MODSUtil.wrapInMyCoReFrame(mods, config.get("base-id"), config.get("status"));
        MODSUtil.setRecordInfo(object, record.getForeignId(), record.getConfigId());

        Document metadata = new Document(MODSUtil.getMetadata(object).detach());

        Document existingMetadata = restAPIService.getObject(target, objectInfo.getMycoreId());
        List<Element> existingIdentifier = MODSUtil.getRegisteredIdentifier(existingMetadata);
        MODSUtil.insertIdentifiers(metadata, existingIdentifier);
        MODSUtil.sortMODSInMetadataElement(metadata);

        restAPIService.putObjectMetadata(target, objectInfo.getMycoreId(), metadata);

        return true;
    }

    private String convertToMods(MyCoReTargetConfiguration target, ForeignEntity record, boolean free, Document picaXml)
        throws TransformerException {
        String resultStr;

        Pica2ModsConfig pica2ModsConfig = new Pica2ModsConfig();
        pica2ModsConfig.setUnapiUrl("https://unapi.k10plus.de/");
        pica2ModsConfig.setMycoreUrl(target.getUrl());
        pica2ModsConfig.setCatalogs(new HashMap<>());

        Pica2ModsManager pica2ModsManager = new Pica2ModsManager(pica2ModsConfig);

        TransformerFactory transformerFactory
            = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", this.getClass().getClassLoader());
        transformerFactory.setURIResolver(new Pica2ModsXSLTURIResolver(pica2ModsManager));
        transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

        ClassLoader var10002 = this.getClass().getClassLoader();

        String stylesheetName = config.get("stylesheet");
        Source xsl = new StreamSource(var10002.getResourceAsStream(stylesheetName));
        xsl.setSystemId(stylesheetName);
        Transformer transformer = transformerFactory.newTransformer(xsl);
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.setParameter("WebApplicationBaseURL", pica2ModsConfig.getMycoreUrl());
        transformer.setParameter("RestrictedAccess", !free ? "true" : "false");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        transformer.transform(new JDOMSource(picaXml), result);
        resultStr = baos.toString(StandardCharsets.UTF_8);
        return resultStr;
    }

    @Override
    public List<Mapping> checkMapping(MyCoReTargetConfiguration target, ForeignEntity record) {
        return Collections.emptyList();
    }

    @Override
    public List<String> listMissingFiles(MyCoReTargetConfiguration target, MyCoReObjectInfo info,
        ForeignEntity record) throws IOException, URISyntaxException {
        // check files
        var davPath = config.get("file-path");
        var path = Paths.get(davPath);

        var newFilesPath = config.get("new-file-path");
        var newPath = Paths.get(newFilesPath);

        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        var filePPN = Stream.of(PicaUtils.getPicaField(picaXML, "003@", "0"),
            PicaUtils.getPicaField(picaXML, "007G", "0"))
            .flatMap(s -> s)
            .distinct()
            .collect(Collectors.toList());

        List<Path> files = resolveFiles(record, filePPN, path, newPath);

        Document object = restAPIService.getObject(target, info.getMycoreId());
        List<String> derivateIDs = MyCoReUtil.getDerivateIDs(object);
        HashSet<Path> notExistingFiles = new HashSet<>(files);
        for (String derivateID : derivateIDs) {
            MyCoReFileListDirectory fileListRepository
                = restAPIService.getFiles(target, info.getMycoreId(), derivateID);
            if (fileListRepository == null || fileListRepository.getFiles() == null) {
                log.error("No files found for record {} in derivate {}", record.getId(), derivateID);
                continue;
            }
            fileListRepository.getFiles().stream()
                .map(MyCoReFileListFile::getName)
                .forEach(ftrm -> {
                    notExistingFiles.removeIf(p -> p.getFileName().toString().equals(ftrm));
                });
        }
        if (!derivateIDs.isEmpty() && !notExistingFiles.isEmpty()) {
            log.info("Missing files but derivate present for record {} in derivate {} (skip em)", record.getId(),
                derivateIDs);
            return List.of();
        }
        return new ArrayList<>(notExistingFiles).stream().map(p -> p.getFileName().toString()).toList();
    }

    public List<String> fixMissingFiles(MyCoReTargetConfiguration target, MyCoReObjectInfo info, ForeignEntity record)
        throws IOException, URISyntaxException {
        // check files
        var davPath = config.get("file-path");
        var path = Paths.get(davPath);

        var newFilesPath = config.get("new-file-path");
        var newPath = Paths.get(newFilesPath);

        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        var filePPN = Stream.of(PicaUtils.getPicaField(picaXML, "003@", "0"),
            PicaUtils.getPicaField(picaXML, "007G", "0"))
            .flatMap(s -> s)
            .distinct()
            .collect(Collectors.toList());

        List<Path> files = resolveFiles(record, filePPN, path, newPath);

        Document object = restAPIService.getObject(target, info.getMycoreId());
        List<String> derivateIDs = MyCoReUtil.getDerivateIDs(object);
        HashSet<Path> notExistingFiles = new HashSet<>(files);
        for (String derivateID : derivateIDs) {
            MyCoReFileListDirectory fileListRepository
                = restAPIService.getFiles(target, info.getMycoreId(), derivateID);
            if (fileListRepository == null || fileListRepository.getFiles() == null) {
                log.error("No files found for record {} in derivate {}", record.getId(), derivateID);
                continue;
            }
            fileListRepository.getFiles().stream()
                .map(MyCoReFileListFile::getName)
                .forEach(ftrm -> {
                    notExistingFiles.removeIf(p -> p.getFileName().toString().equals(ftrm));
                });
        }

        if (notExistingFiles.isEmpty()) {
            log.info("No missing files for record {}", record.getId());
            return List.of();
        }

        String derivateID = derivateIDs.stream().findFirst().orElse(null);
        if (derivateID == null) {
            List<String> fileURLs = PicaUtils.getPicaField(picaXML, "017C", "u")
                .toList();

            boolean free = !fileURLs.isEmpty() && isPublic(record.getForeignId(), picaXML.getRootElement());
            derivateID
                = createDerivate(target, record, free, info.getMycoreId(), files.get(0).getFileName().toString());
        }

        for (Path p : files) {
            log.info("Import file {} to {}", p.getFileName(), info.getMycoreId());
            restAPIService.putFiles(target, info.getMycoreId(), derivateID, p.getFileName().toString(),
                Files.newInputStream(p));
            log.info("Imported file {} to {}", p.getFileName(), info.getMycoreId());
        }

        return files.stream().map(p -> p.getFileName().toString()).toList();
    }

    @Override
    public List<String> listImportableFiles(MyCoReTargetConfiguration target, ForeignEntity record)
        throws IOException, URISyntaxException {
        // check files
        var davPath = config.get("file-path");
        var path = Paths.get(davPath);

        var newFilesPath = config.get("new-file-path");
        var newPath = Paths.get(newFilesPath);

        // this should be the ppn from the record in the field 003S@0 or 007G@0
        Document picaXML = getForeignEntityDocument(record);
        var filePPN = Stream.of(PicaUtils.getPicaField(picaXML, "003@", "0"),
            PicaUtils.getPicaField(picaXML, "007G", "0"))
            .flatMap(s -> s)
            .distinct()
            .collect(Collectors.toList());

        List<Path> files = resolveFiles(record, filePPN, path, newPath);
        return files.stream().map(p -> p.getFileName().toString()).toList();
    }

    public boolean shouldNotBeImported(ForeignEntity record, MyCoReObjectInfo info) throws IOException, JDOMException {
        String metadata = record.getMetadata();
        SAXBuilder saxBuilder = new SAXBuilder();

        Document doc = saxBuilder.build(new StringReader(metadata));
        DFISRURecordFilter dfisruRecordFilter = new DFISRURecordFilter();
        boolean filter = dfisruRecordFilter.filter(doc, null);
        //log.info("Record {} with mycore id {} should not have been imported", record.getId(), info.getMycoreId());
        return !filter;
    }

    @Override
    public void setConfig(Map<String, String> importerConfig) {
        this.config = importerConfig;
    }
}
