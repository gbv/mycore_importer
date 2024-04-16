package de.vzg.oai_importer.foreign;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.stereotype.Service;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;

@Service
public interface Harvester<T extends Configuration>  {

    public List<ForeignEntity> update(String configID, T source, boolean onlyMissing) throws IOException, URISyntaxException;

}
