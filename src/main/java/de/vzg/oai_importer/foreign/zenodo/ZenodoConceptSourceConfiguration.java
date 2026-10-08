package de.vzg.oai_importer.foreign.zenodo;

/**
 * Configuration of a Zenodo source which provides one record per concept (parent) instead of one record per version.
 * The metadata of each record is the metadata of the latest version of the concept.
 */
public class ZenodoConceptSourceConfiguration extends ZenodoSourceConfiguration {

    @Override
    public String getHarvester() {
        return ZenodoConceptHarvester.ZENODO_CONCEPT_HARVESTER;
    }

    @Override
    public String getName() {
        return getUrl() + " " + getCommunity() + " (concepts)";
    }
}
