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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.PicaUtils;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import lombok.extern.log4j.Log4j2;

@Service(PPNListHarvester.PPN_LIST_HARVESTER)
@Log4j2
public class PPNListHarvester implements Harvester<PPNListConfiguration> {

    public static final String PPN_LIST_HARVESTER = "PPNListHarvester";

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

        List<String> al = new ArrayList<>(ppns);
        var count1 = new AtomicInteger(al.size());

        al = al.stream()
                .filter(ppn -> {
                    if(!onlyMissing){
                        return true;
                    }
                    //log.info("Checking PPN " + ppn + " (" + count1.decrementAndGet() + " remaining)");
                    return recordRepository.findFirstByConfigIdAndForeignId(configID, ppn) == null;
                }).collect(Collectors.toList());

        var count = new AtomicInteger(al.size());
        for (String ppn : al) {
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
                    log.error("Error while fetching PPN " + ppn, e);
                    continue;
                }
            } catch (MalformedURLException e) {
                log.error("Error while fetching PPN " + ppn, e);
                continue;
            }

            try (var sr = new StringReader(record.getMetadata())) {
                Document doc = new SAXBuilder().build(sr);
                Element rootElement = doc.getRootElement();
                List<OffsetDateTime> modifiedList = PicaUtils.getModifiedDate(rootElement);
                modifiedList.stream().findFirst().ifPresent(record::setDatestamp);
            } catch (IOException | JDOMException e) {
                log.error("Error while parsing PPN " + ppn, e);
                continue;
            }

            recordRepository.save(record);
            result.add(record);
        }

        log.info("Completed processing PPNs");

        return result;
    }
}
