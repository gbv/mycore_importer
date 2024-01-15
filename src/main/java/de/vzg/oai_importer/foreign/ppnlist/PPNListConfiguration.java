package de.vzg.oai_importer.foreign.ppnlist;

import java.util.List;
import java.util.stream.Collectors;

import de.vzg.oai_importer.foreign.Configuration;
import lombok.Data;

@Data
public class PPNListConfiguration implements Configuration {

    private List<String> filePaths;

    @Override
    public String getName() {
        return "PPNLists: " + filePaths.stream().map(s -> s.substring(s.lastIndexOf('/') + 1)).collect(Collectors.joining(", "));
    }

    @Override
    public String getHarvester() {
        return PPNListHarvester.PPN_LIST_HARVESTER;
    }

    @Override
    public String getUrl() {
        return "-";
    }
}
