package de.vzg.oai_importer.mycore.api.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public interface TransferLayer {

    TransferResult post(String url,
                        String bodyContentType,
                        InputStream body,
                        Map<String, String> headers,
                        Map<String, List<String>> params) throws URISyntaxException, IOException;

    TransferResult put(String url,
                       String bodyContentType,
                       InputStream body,
                       Map<String, String> headers,
                       Map<String, List<String>> params) throws URISyntaxException, IOException;

    TransferResult get(String url,
                    Map<String, String> headers,
                    Map<String, List<String>> params) throws IOException, URISyntaxException;

}
