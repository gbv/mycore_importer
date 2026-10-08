package de.vzg.oai_importer.importer;

import org.mycore.libmeta.mods.model.Mods;
import org.mycore.libmeta.mods.model._toplevel.Identifier;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestRecord;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import lombok.extern.log4j.Log4j2;

/**
 * Imports Zenodo concepts (see ZenodoConceptHarvester) as one MyCoRe object per concept. There is no grouping object,
 * because versions are not imported. The DOI of the object is the concept DOI, which always resolves to the latest
 * version.
 */
@Service("ZenodoConcept2MyCoReImporter")
@Log4j2
public class ZenodoConcept2MyCoReImporter extends Zenodo2MyCoReImporter {

    @Override
    protected void handleDOI(ZenodoRestRecord restRecord, Mods.Builder mods) {
        String doi = restRecord.getConceptdoi();
        if (doi == null) {
            log.warn("Record {} has no concept DOI, using the DOI of the version", restRecord.getId());
            super.handleDOI(restRecord, mods);
            return;
        }
        mods.addContent(Identifier.builderForIdentifier().content(doi).type("doi").build());
    }

    @Override
    protected void handleGrouping(MyCoReTargetConfiguration target, ForeignEntity recordEntity,
        ZenodoRestRecord restRecord, Mods.Builder mods) {
        // no versions, no grouping
    }
}
