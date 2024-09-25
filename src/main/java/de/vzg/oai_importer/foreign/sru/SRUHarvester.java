/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vzg.oai_importer.foreign.sru;

import static de.vzg.oai_importer.Namespaces.PICA_NAMESPACE;
import static de.vzg.oai_importer.Namespaces.SRU_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.PicaUtils;
import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import lombok.extern.log4j.Log4j2;

@Service(SRUHarvester.SRU_HARVESTER)
@Log4j2
public class SRUHarvester implements Harvester<SRUConfiguration> {

    @Autowired
    ApplicationContext context;

    public static final String RECORD_DATA_XPATH = "/zs:searchRetrieveResponse/zs:records/zs:record/zs:recordData";
    public static final String SRU_HARVESTER = "SRUHarvester";
    private static final String NUMBER_OF_RECORDS_XPATH = "/zs:searchRetrieveResponse/zs:numberOfRecords";

    @Autowired
    private ForeignEntityRepository recordRepository;

    public String buildLink(SRUConfiguration source, LocalDate day, int startRecord, int maximumRecords) {
        String queryPattern = source.getQueryPattern();
        String url = source.getUrl();

        String queryResult = queryPattern.replace("{date}", day.toString());
        String encode = URLEncoder.encode(queryResult, StandardCharsets.UTF_8);
        return url + "?version=1.1&operation=searchRetrieve&query=" + encode + "&maximumRecords=" + maximumRecords +
            "&recordSchema=picaxml&startRecord=" + startRecord;
    }

    public List<LocalDate> getDaysSince(LocalDate from, LocalDate until) {
        if(from.isAfter(until)) {
            return List.of();
        }
        return from.datesUntil(until).toList();
    }

    public SRUResponse harvest(String link) throws IOException {
        HttpGet get = new HttpGet(link);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            return httpclient.execute(get, response -> {
                try (InputStream is = response.getEntity().getContent()) {
                    Document build = new SAXBuilder().build(is);
                    int numberOfRecords = extractNumberOfRecords(build);
                    List<Document> records = extractRecords(build);
                    return new SRUResponse(numberOfRecords, records);
                } catch (JDOMException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public int extractNumberOfRecords(Document document) {
        XPathExpression<Element> numberOfRecordsXPath = XPathFactory.instance().compile(NUMBER_OF_RECORDS_XPATH,
            Filters.element(), null, SRU_NAMESPACE);
        Element element = numberOfRecordsXPath.evaluateFirst(document);
        return Integer.parseInt(element.getText());
    }

    public List<Document> extractRecords(Document document) {
        XPathExpression<Element> recordsXPath
            = XPathFactory.instance().compile(RECORD_DATA_XPATH + "/pica:record", Filters.element(),
                null, SRU_NAMESPACE, PICA_NAMESPACE);

        List<Element> evaluate = recordsXPath.evaluate(document);

        return evaluate.stream().map(Element::clone)
            .map(Document::new)
            .toList();
    }

    @Override
    public List<ForeignEntity> update(String configID, SRUConfiguration source, boolean onlyMissing)
        throws IOException, URISyntaxException {

        LocalDate oldestDate = source.getOldestDate();
        OffsetDateTime entity = recordRepository.getNewestDatestamp(configID);
        if (entity != null) {
            oldestDate = entity.toLocalDate();
        }

        if (source.getDateOverwrite() != null) {
            oldestDate = source.getDateOverwrite();
        }

        List<ForeignEntity> result = new ArrayList<>();

        List<LocalDate> days = getDaysSince(oldestDate, source.getNewestDate() == null ?
                LocalDate.now().plusDays(source.getDayOffset()) : source.getNewestDate());
        for (LocalDate day : days) {
            SRUResponse resp = null;
            String link = null;
            try {
                int startRecord = 1;
                do {
                    link = buildLink(source, day, startRecord, 100);
                    log.info("Harvesting from {}", link);
                    resp = harvest(link);
                    for (Document picaRecord : resp.records()) {
                        if(filterRecord(source, picaRecord, day)) {
                            processRecord(configID, picaRecord, result, day);
                        }
                    }
                    startRecord += 100;
                } while (resp.numberOfRecords() > startRecord);

            } catch (Exception e) {
                log.error("Error while harvesting from {}", link != null ? link : "null", e);
            }
        }

        return result;
    }

    private boolean filterRecord(SRUConfiguration config, Document picaRecord, LocalDate day) {
        Optional<String> ppnField = PicaUtils.getPicaField(picaRecord, "003@", "0").findFirst();
        if (ppnField.isEmpty()) {
            log.warn("No PPN found in record {}", new XMLOutputter().outputString(picaRecord));
            return false;
        }

        if (config.getRecordFilterService() == null || config.getRecordFilterService().isBlank()) {
            return true;
        }

        SRURecordFilter filter = context.getBean(config.getRecordFilterService(), SRURecordFilter.class);
        String recordName = ppnField.get();
        log.info("Checking record {}", recordName);
        boolean filterResult = filter.filter(picaRecord, day);
        if (!filterResult) {
            log.info("Record {} filtered out", recordName);
        }
        return filterResult;
    }

    private boolean processRecord(String configID, Document picaRecord, List<ForeignEntity> result, LocalDate day) {
        String metadata = new XMLOutputter().outputString(picaRecord);

        Optional<String> ppnField = PicaUtils.getPicaField(picaRecord, "003@", "0").findFirst();
        if (ppnField.isEmpty()) {
            log.warn("No PPN found in record {}", new XMLOutputter().outputString(picaRecord));
            return false;
        }
        String ppn = ppnField.get();

        ForeignEntity foreignEntity
            = Optional.ofNullable(recordRepository.findFirstByConfigIdAndForeignId(configID, ppn))
                .orElseGet(ForeignEntity::new);

        foreignEntity.setConfigId(configID);
        foreignEntity.setMetadata(metadata);

        foreignEntity.setForeignId(ppn);

        List<OffsetDateTime> modifiedList = PicaUtils.getModifiedDate(picaRecord.getRootElement());
        modifiedList.stream().findFirst().ifPresent(foreignEntity::setDatestamp);

        foreignEntity.setDeleted(false);

        recordRepository.save(foreignEntity);
        result.add(foreignEntity);
        return true;
    }

    public record SRUResponse(int numberOfRecords, List<Document> records) {
    }
}
