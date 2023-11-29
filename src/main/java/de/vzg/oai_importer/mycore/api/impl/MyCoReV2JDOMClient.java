package de.vzg.oai_importer.mycore.api.impl;

import java.util.List;
import java.util.Map;

import org.jdom2.Document;

import de.vzg.oai_importer.mycore.api.MCRV2RestClient;
import de.vzg.oai_importer.mycore.api.transfer.TransferLayer;

public class MyCoReV2JDOMClient extends MCRV2RestClient<Document> {
    public MyCoReV2JDOMClient(TransferLayer transferLayer) {
        super(transferLayer, new JDOM2ResultMapper());
    }

    @Override
    public void adaptRequestParameters(Map<String, List<String>> params, Map<String, String> headers) {
        headers.computeIfAbsent("Accept", k -> "application/xml");
    }
}
