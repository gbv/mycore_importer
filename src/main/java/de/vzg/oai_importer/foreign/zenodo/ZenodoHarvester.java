package de.vzg.oai_importer.foreign.zenodo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import lombok.extern.log4j.Log4j2;

@Service(ZenodoHarvester.ZENODO_HARVESTER)
@Log4j2
public class ZenodoHarvester implements Harvester<ZenodoSourceConfiguration> {

    public static final String ZENODO_HARVESTER = "ZenodoHarvester";

    @Autowired
    private ForeignEntityRepository recordRepository;

    private static String buildURIComplete(String url, Map<String, List<String>> params) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        params.forEach((k, v) -> {
            v.forEach(vv -> uriBuilder.addParameter(k, vv));
        });
        return uriBuilder.build().toString();
    }

    @Override
    public List<ForeignEntity> update(String configID, ZenodoSourceConfiguration source)
        throws IOException, URISyntaxException {
        List<ForeignEntity> entities = new ArrayList<>();
        ForeignEntity first = recordRepository.findFirstByConfigIdOrderByDatestampDesc(configID);

        OffsetDateTime datestamp = Optional.ofNullable(first).map(ForeignEntity::getDatestamp)
            .orElseGet(() -> OffsetDateTime.now().minusYears(30));

        String newestLocal = datestamp.format(DateTimeFormatter.ISO_LOCAL_DATE);

        Map<String, List<String>> parameters = new HashMap<>();

        parameters.put("communities", List.of(source.getCommunity()));
        parameters.put("sort", List.of("mostrecent"));
        parameters.put("q", List.of("updated:[" + newestLocal + " TO *]"));
        parameters.put("size", List.of("1000"));

        AtomicInteger remainingRequests = new AtomicInteger(1);
        AtomicInteger rateLimitReset = new AtomicInteger(0);
        AtomicInteger currentPage = new AtomicInteger(0);
        AtomicInteger maxPages = new AtomicInteger(-1);
        int hitsPerPage = 25;
        boolean abort = false;


        while (remainingRequests.get() > 0 && !abort) {
            parameters.put("page", List.of(String.valueOf(currentPage.incrementAndGet())));
            parameters.put("size", List.of(String.valueOf(hitsPerPage)));
            parameters.put("all_versions", List.of("true"));
            HttpGet get = new HttpGet(buildURIComplete(source.getUrl() + "api/records", parameters));
            log.info("Requesting page {} of {} with url {}", currentPage.get(), maxPages.get() == -1 ? "?" : maxPages.get(), get.getUri());
            get.addHeader("Accept", "application/json");
            get.addHeader("User-Agent", "MyCoRe Importer");
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                abort = !httpclient.execute(get, (ClassicHttpResponse response) -> {
                    Header rateLimitRemainingHeader = getSingleHeader(response, "x-ratelimit-remaining");
                    remainingRequests.set(Integer.parseInt(rateLimitRemainingHeader.getValue()));

                    Header rateLimitResetHeader = getSingleHeader(response, "x-ratelimit-reset");
                    rateLimitReset.set(Integer.parseInt(rateLimitResetHeader.getValue()));

                    if(response.getCode() == 200) {
                        InputStream content = response.getEntity().getContent();
                        int max = processForeignEntities(configID, datestamp, content, entities);
                        maxPages.set((int) Math.ceil(((double) max) / 25.0));
                        return maxPages.get() > currentPage.get();
                    } else {
                        // handle error
                        log.error("Error while harvesting: {}", response.getReasonPhrase());
                        return false;
                    }
                });
                if(remainingRequests.get()<0){
                    log.warn("Rate limit exceeded! Resets in {}", rateLimitReset.get());
                }
            }
        }

        return entities;
    }

    private static Header getSingleHeader(ClassicHttpResponse response, String headerName) {
        return Arrays.stream(response.getHeaders(headerName)).findFirst().get();
    }

    private int processForeignEntities(String configID, OffsetDateTime datestamp, InputStream inputStream,
        List<ForeignEntity> entities) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode node = objectMapper.readTree(inputStream);
        // Read the JSON as a tree, because we want the every hit as pure JSON not filtered by a POJO
        // That will be saved in the database

        ObjectNode hitsObject = Optional.ofNullable(node.get("hits"))
            .filter(JsonNode::isObject)
            .map(ObjectNode.class::cast)
            .orElseThrow(() -> new IOException("No hits object found in response"));

        int total = hitsObject.get("total").asInt();

        ArrayNode hitsArray = Optional.ofNullable(hitsObject.get("hits"))
            .filter(JsonNode::isArray)
            .map(ArrayNode.class::cast)
            .orElseThrow(() -> new IOException("No hits array found in response"));

        ObjectWriter prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();
        for (int i = 0; i < hitsArray.size(); i++) {
            ObjectNode hit = Optional.ofNullable(hitsArray.get(i))
                .filter(JsonNode::isObject)
                .map(ObjectNode.class::cast)
                .orElseThrow(() -> new IOException("Hit is not an object"));



            String pureMetadata = prettyPrinter.writeValueAsString(hit);

            String modifiedText = hit.get("modified").asText();
            String id = String.valueOf(hit.get("id").asInt());
            Instant modified =Instant.parse(modifiedText);

            if (modified.isAfter(datestamp.toInstant())) {
                String recordId = String.valueOf(id);
                // check if record already exists in database, to prevent duplicates
                ForeignEntity entity
                    = Optional.ofNullable(recordRepository.findFirstByConfigIdAndForeignId(configID, recordId))
                        .orElseGet(ForeignEntity::new);

                entity.setConfigId(configID);
                entity.setForeignId(recordId);
                entity.setDatestamp(OffsetDateTime.ofInstant(modified, ZoneOffset.UTC));
                entity.setMetadata(pureMetadata);
                entity.setDeleted(false);
                recordRepository.save(entity);
                entities.add(entity);
            }
        }
        return total;
    }

    record Result(int hitsInRequest, int maxHits){}
}
