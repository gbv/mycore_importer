package de.vzg.oai_importer.foreign.zenodo;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.log4j.Log4j2;

/**
 * Harvests only the latest version of each Zenodo record and identifies it by the id of its concept (parent) record.
 * Therefore, there is exactly one entity per concept and new versions update the existing entity.
 */
@Service(ZenodoConceptHarvester.ZENODO_CONCEPT_HARVESTER)
@Log4j2
public class ZenodoConceptHarvester extends ZenodoHarvester {

    public static final String ZENODO_CONCEPT_HARVESTER = "ZenodoConceptHarvester";

    @Override
    protected boolean isAllVersions() {
        return false;
    }

    @Override
    protected String getForeignId(ObjectNode hit) {
        return Optional.ofNullable(hit.get("conceptrecid"))
            .filter(JsonNode::isValueNode)
            .map(JsonNode::asText)
            .or(() -> Optional.ofNullable(hit.at("/metadata/relations/version/0/parent/pid_value"))
                .filter(JsonNode::isValueNode)
                .map(JsonNode::asText))
            .orElseGet(() -> {
                log.warn("Could not find concept id of record {}, using the record id", hit.get("id"));
                return super.getForeignId(hit);
            });
    }
}
