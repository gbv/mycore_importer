package de.vzg.oai_importer.mycore.api.transfer;

import java.util.List;
import java.util.Map;

public interface RequestParameterAdapter {
    void adaptRequestParameters(Map<String, List<String>> params, Map<String, String> headers);
}
