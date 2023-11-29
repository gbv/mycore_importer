package de.vzg.oai_importer.foreign.oai;

import de.vzg.oai_importer.foreign.Harvester;
import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.jpa.ForeignEntityRepository;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.oai.pmh.OAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service(OAIDatabaseHarvester.OAI_HARVESTER)
public class OAIDatabaseHarvester implements Harvester<OAISourceConfiguration> {

    public static final String OAI_HARVESTER = "OAIHarvester";
    @Autowired
    private OAIHarvesterService harvesterService;

    @Autowired
    private ForeignEntityRepository recordRepository;

    public List<ForeignEntity> update(String configID, OAISourceConfiguration source) throws IOException {
        ForeignEntity first = recordRepository.findFirstByConfigIdOrderByDatestampDesc(configID);

        List<ForeignEntity> updatedRecords = new ArrayList<>();

        try {
            harvesterService.harvest(source,
                first == null ? OffsetDateTime.now().minus(30, ChronoUnit.YEARS) : first.getDatestamp(),
                OffsetDateTime.now(), (header, record) -> {
                    if (first == null || header.getDatestamp().atOffset(ZoneOffset.UTC)
                        .isAfter(first.getDatestamp())) {
                        ForeignEntity entity = new ForeignEntity();
                        entity.setConfigId(configID);
                        entity.setForeignId(header.getId());
                        entity.setDatestamp(header.getDatestamp().atOffset(ZoneOffset.UTC));
                        boolean deleted = header.isDeleted();

                        if (!header.isDeleted()) {
                            String metadataAsString = new XMLOutputter(Format.getPrettyFormat())
                                .outputString(record.getMetadata().toXML());
                            entity.setMetadata(metadataAsString);
                        }

                        entity.setDeleted(deleted);
                        recordRepository.save(entity);

                        updatedRecords.add(entity);
                    }
                });
        } catch (OAIException e) {
            throw new IOException(e);
        }

        return updatedRecords;
    }
}
