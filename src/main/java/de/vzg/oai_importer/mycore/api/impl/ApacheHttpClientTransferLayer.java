package de.vzg.oai_importer.mycore.api.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import de.vzg.oai_importer.mycore.api.transfer.TransferLayer;
import de.vzg.oai_importer.mycore.api.transfer.TransferResult;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ApacheHttpClientTransferLayer implements TransferLayer {

    private static String buildURIComplete(String url, Map<String, List<String>> params) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        params.forEach((k, v) -> {
            v.forEach(vv -> uriBuilder.addParameter(k, vv));
        });
        return uriBuilder.build().toString();
    }

    @Override
    public TransferResult post(String url, String bodyContentType, InputStream body, Map<String, String> headers,
                               Map<String, List<String>> params) throws URISyntaxException, IOException {
        String requestURL = buildURIComplete(url, Collections.emptyMap());

        log.info("Requesting (POST) {}", requestURL);

        HttpPost post = new HttpPost(requestURL);
        headers.forEach(post::addHeader);
        if (body != null) {
            post.setEntity(new InputStreamEntity(body, ContentType.parse(bodyContentType)));
        }
        if (!params.isEmpty()) {
            List<BasicNameValuePair> list = params.entrySet().stream().flatMap(
                e -> e.getValue().stream().map(
                    v -> new BasicNameValuePair(e.getKey(), v)))
                .toList();
            post.setEntity(new UrlEncodedFormEntity(list));
        }
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            return httpclient.execute(post, (ClassicHttpResponse response) -> {
                InputStream content = response.getEntity().getContent();
                byte[] bytes = content.readAllBytes();
                return new TransferResult(new ByteArrayInputStream(bytes), response.getCode(),
                    response.getReasonPhrase(), convertHeaders(response));
            });
        }
    }

    private static Map<String, String> convertHeaders(ClassicHttpResponse response) {
        return  Arrays.stream(response.getHeaders())
                .collect(Collectors.toMap(Header::getName, Header::getValue));
    }

    @Override
    public TransferResult put(String url, String bodyContentType, InputStream body, Map<String, String> headers,
                              Map<String, List<String>> params) throws URISyntaxException, IOException {
        String requestURL = buildURIComplete(url, params);
        log.info("Requesting (PUT) {}", requestURL);
        HttpPut put = new HttpPut(requestURL);
        headers.forEach(put::addHeader);
        ContentType contentType = bodyContentType == null ? null : ContentType.parse(bodyContentType);
        InputStreamEntity entity = new InputStreamEntity(body, contentType);
        put.setEntity(entity);
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            return httpclient.execute(put, (ClassicHttpResponse response) -> {
                byte[] bytes;

                if(response.getEntity() != null) {
                    InputStream content = response.getEntity().getContent();
                    bytes = content.readAllBytes();
                } else {
                    bytes = new byte[0];
                }

                return new TransferResult(new ByteArrayInputStream(bytes), response.getCode(),
                    response.getReasonPhrase(), convertHeaders(response));
            });
        }
    }

    @Override
    public TransferResult get(String url, Map<String, String> headers, Map<String, List<String>> params)
        throws IOException,
        URISyntaxException {
        String requestURL = buildURIComplete(url, params);
        log.info("Requesting (GET) {}", requestURL);
        HttpGet get = new HttpGet(requestURL);
        headers.forEach(get::addHeader);
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            return httpclient.execute(get, (ClassicHttpResponse response) -> {
                InputStream content = response.getEntity().getContent();
                byte[] bytes = content.readAllBytes();
                return new TransferResult(new ByteArrayInputStream(bytes), response.getCode(),
                    response.getReasonPhrase(), convertHeaders(response));
            });
        }
    }

}
