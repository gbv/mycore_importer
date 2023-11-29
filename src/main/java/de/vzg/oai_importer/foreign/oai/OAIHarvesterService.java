package de.vzg.oai_importer.foreign.oai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.Granularity;
import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.OAIDataList;
import org.mycore.oai.pmh.OAIException;
import org.mycore.oai.pmh.Record;
import org.mycore.oai.pmh.harvester.Harvester;
import org.mycore.oai.pmh.harvester.HarvesterBuilder;
import org.mycore.oai.pmh.harvester.impl.JAXBHarvesterFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.function.BiConsumer;

@Service()
public class OAIHarvesterService {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String OAI_HARVESTER = "OAIHarvester";

    OAIHarvesterService() {
        HarvesterBuilder.setHarvesterFactory(new JAXBHarvesterFactory());
    }

    /**
     * Harvests all records from the given source and calls the consumer for each record.
     * @param source the source to harvest from
     * @param from the date to start harvesting from or null
     * @param until the date to stop harvesting or null
     * @param consumer the consumer to call for each record
     * @throws OAIException if the harvesting fails
     */
    public void harvest(OAISourceConfiguration source,
        OffsetDateTime from,
        OffsetDateTime until,
        BiConsumer<Header, Record> consumer) throws OAIException {

        Harvester harvester = HarvesterBuilder.createNewInstance(source.getUrl());
        Granularity granularity = harvester.identify().getGranularity();

        OAIDataList<Record> records = null;

        LOGGER.info("Harvesting from {} to {} from {}", from, until, source.getUrl());
        while (records == null || records.isResumptionTokenSet()) {
            if (records != null) {
                LOGGER.info("Harvesting from {} to {} from {} with resumption token {}", from, until,
                    source.getUrl(), records.getResumptionToken().getToken());
            }

            String fromOrNull = from == null ? null : DateUtils.format(from.toInstant(), granularity);
            String untilOrNull = until == null ? null : DateUtils.format(until.toInstant(), granularity);

            records = records == null
                ? harvester.listRecords(source.getMetadataPrefix(), fromOrNull, untilOrNull, source.getSet())
                : harvester.listRecords(records.getResumptionToken().getToken());
            for (Record record : records) {
                Header header = record.getHeader();
                consumer.accept(header, record);
            }
        }
    }

}
