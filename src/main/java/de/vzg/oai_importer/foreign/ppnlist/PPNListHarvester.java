package de.vzg.oai_importer.foreign.ppnlist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import lombok.extern.log4j.Log4j2;

@Service(PPNListHarvester.PPN_LIST_HARVESTER)
@Log4j2
public class PPNListHarvester implements Harvester<PPNListConfiguration> {

    public static final String PPN_LIST_HARVESTER = "PPNListHarvester";
    Namespace picaxml = Namespace.getNamespace("info:srw/schema/5/picaXML-v1.0");
    @Autowired
    private ForeignEntityRepository recordRepository;

    @Override
    public List<ForeignEntity> update(String configID, PPNListConfiguration source, boolean onlyMissing)
        throws IOException, URISyntaxException {
        HashSet<String> ppns = new HashSet<>();
        var result = new ArrayList<ForeignEntity>();

        source.getFilePaths().forEach(pathStr -> {
            Path path = Paths.get(pathStr);
            try (BufferedReader br = Files.newBufferedReader(path)) {
                br.lines().forEach(ppns::add);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        var al = new ArrayList<>(ppns);
        var count1 = new AtomicInteger(al.size());

        List<String> missing = al.stream()
                .filter(ppn -> {
                    if(!onlyMissing){
                        return true;
                    }
                    log.info("Checking PPN " + ppn + " (" + count1.decrementAndGet() + " remaining)");
                    return recordRepository.findFirstByConfigIdAndForeignId(configID, ppn) == null;
                }).toList();

        var count = new AtomicInteger(missing.size());
        missing.stream()
            .parallel()
            .forEach(ppn -> {
                log.info("Processing PPN " + ppn + " (" + count.decrementAndGet() + " remaining)");
                ForeignEntity record
                    = Optional.ofNullable(recordRepository.findFirstByConfigIdAndForeignId(configID, ppn))
                        .orElseGet(ForeignEntity::new);
                record.setConfigId(configID);
                record.setForeignId(ppn);
                record.setDeleted(false);
                record.setDatestamp(OffsetDateTime.now());

                try {
                    URL url = new URL("https://unapi.k10plus.de/?id=gvk:ppn:" + ppn + "&format=picaxml");
                    try (var is = url.openStream(); var isr = new InputStreamReader(is);
                        var br = new BufferedReader(isr)) {
                        String metadata = br.lines().collect(Collectors.joining("\n"));
                        record.setMetadata(metadata);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

                try (var sr = new StringReader(record.getMetadata())) {
                    Document doc = new SAXBuilder().build(sr);
                    Element rootElement = doc.getRootElement();
                    rootElement.getChildren("datafield", picaxml)
                        .stream()
                        .filter(e -> e.getAttributeValue("tag").equals("001B"))
                        .findFirst().ifPresent(element -> {
                            String p0
                                = element.getChildren().stream().filter(e -> e.getAttributeValue("code").equals("0"))
                                    .findFirst().get().getText();

                            String time
                                = element.getChildren().stream().filter(e -> e.getAttributeValue("code").equals("t"))
                                    .findFirst().get().getText();

                            String date = p0.split(":")[1];

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss.SSS");
                            OffsetDateTime dateTime = LocalDateTime.parse(date + " " + time, formatter)
                                .atOffset(OffsetDateTime.now().getOffset());
                            record.setDatestamp(dateTime);
                        });
                } catch (IOException | JDOMException e) {
                    throw new RuntimeException(e);
                }

                if (record.getMetadata().length() > 512000) {
                    log.warn("Metadata too long for PPN " + ppn);
                    return;
                }

                recordRepository.save(record);
                result.add(record);
            });

        return result;
    }
}
