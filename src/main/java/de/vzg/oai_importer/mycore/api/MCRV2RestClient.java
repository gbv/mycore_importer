package de.vzg.oai_importer.mycore.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.vzg.oai_importer.mycore.api.model.MyCoReObjectList;
import de.vzg.oai_importer.mycore.api.transfer.RequestParameterAdapter;
import de.vzg.oai_importer.mycore.api.transfer.ResultMapper;
import de.vzg.oai_importer.mycore.api.transfer.TransferLayer;
import de.vzg.oai_importer.mycore.api.transfer.TransferResult;

public abstract class MCRV2RestClient<T> implements RequestParameterAdapter {

    public static final String API_V_2_OBJECTS = "api/v2/objects";

    public static final String API_V_2_AUTH_LOGIN = "api/v2/auth/login";
    private static final String API_V_2_CLASSIFICATION = "api/v2/classifications";

    private final TransferLayer transferLayer;

    private final ResultMapper<T> resultMapper;

    public MCRV2RestClient(TransferLayer transferLayer,
        ResultMapper<T> resultMapper) {
        this.transferLayer = transferLayer;
        this.resultMapper = resultMapper;
    }

    public String authenticate(String repositoryURL, MCRRestCredentials credentials)
        throws IOException, URISyntaxException {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + credentials.getBase64Encoded());
        TransferResult result = transferLayer.get(repositoryURL + API_V_2_AUTH_LOGIN, headers, new HashMap<>());
        // in this case we dont use the result mapper since the response is always the same no matter what
        // Accept header is set to. We just want to get the token from the response body and return it.

        int i = result.statusCode();
        if (i != 200) {
            throw new RuntimeException("Error while authenticating: " + result.statusMessage());
        }
        try (InputStream inputStream = result.inputStream()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(inputStream);
            JsonNode tokenNode = node.get("access_token");
            JsonNode tokenTypeNode = node.get("token_type");
            if (tokenNode == null) {
                throw new RuntimeException("Error while authenticating: " + result.statusMessage());
            }
            if (tokenTypeNode == null) {
                throw new RuntimeException("Error while authenticating: " + result.statusMessage());
            }
            return tokenTypeNode.asText() + " " + tokenNode.asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MyCoReObjectList listObjects(String repositoryURL, MyCoReObjectQuery query)
        throws IOException, URISyntaxException {
        return listObjects(repositoryURL, query, null);
    }

    public MyCoReObjectList listObjects(String repositoryURL, MyCoReObjectQuery query, String authHeader)
        throws IOException, URISyntaxException {
        HashMap<String, List<String>> parameters = new HashMap<>();
        applyQueryParameters(query, parameters);

        HashMap<String, String> headers = new HashMap<>();
        applyAuth(authHeader, headers);

        adaptRequestParameters(parameters, headers);
        TransferResult result = transferLayer.get(repositoryURL + API_V_2_OBJECTS, headers, parameters);
        int i = result.statusCode();
        if (i != 200) {
            throw new RuntimeException("Error while listing objects: " + result.statusMessage());
        }
        try (InputStream inputStream = result.inputStream()) {
            return resultMapper.mapObjectList(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void applyAuth(String authHeader, HashMap<String, String> headers) {
        if (authHeader != null) {
            headers.put("Authorization", authHeader);
        }
    }

    public T getObject(String repositoryURL, String object, String authHeader) throws IOException, URISyntaxException {
        HashMap<String, List<String>> parameters = new HashMap<>();
        HashMap<String, String> headers = new HashMap<>();
        applyAuth(authHeader, headers);
        adaptRequestParameters(parameters, headers);
        TransferResult result = transferLayer.get(repositoryURL + API_V_2_OBJECTS + "/" + object, headers, parameters);
        int i = result.statusCode();
        if (i != 200) {
            throw new RuntimeException("Error while getting object: " + result.statusMessage());
        }
        try (InputStream inputStream = result.inputStream()) {
            return resultMapper.mapGeneric(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public T getObject(String repositoryURL, String object) throws IOException, URISyntaxException {
        return getObject(repositoryURL, object, null);
    }

    public T getClassification(String repositoryURL, String classification) throws IOException, URISyntaxException {
        HashMap<String, List<String>> parameters = new HashMap<>();
        HashMap<String, String> headers = new HashMap<>();
        adaptRequestParameters(parameters, headers);
        TransferResult result = transferLayer.get(repositoryURL + API_V_2_CLASSIFICATION + "/" + classification, headers, parameters);
        int i = result.statusCode();
        if (i != 200) {
            throw new RuntimeException("Error while getting object: " + result.statusMessage());
        }
        try (InputStream inputStream = result.inputStream()) {
            return resultMapper.mapGeneric(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String postObject(String url,
                             String authHeader,
                             ByteArrayInputStream objectContent
            , String contentType) throws IOException, URISyntaxException {

        HashMap<String, List<String>> parameters = new HashMap<>();
        HashMap<String, String> headers = new HashMap<>();

        applyAuth(authHeader, headers);
        adaptRequestParameters(parameters, headers);

        TransferResult result = transferLayer.post(url + API_V_2_OBJECTS + "/", contentType,
                objectContent, headers, parameters);
        int i = result.statusCode();

        if (!(i == 200 || i == 201)) {
            throw new RuntimeException("Error while putting object: " + result.statusMessage());
        }

        try (InputStream inputStream = result.inputStream()) {
            inputStream.readAllBytes();
        }
        return result.headers().get("Location");
    }
    public String putObject(String repositoryURL,
        String objectID,
        String authHeader,
        InputStream objectContent,
        String contentType)
        throws IOException, URISyntaxException {

        HashMap<String, List<String>> parameters = new HashMap<>();
        HashMap<String, String> headers = new HashMap<>();

        applyAuth(authHeader, headers);
        adaptRequestParameters(parameters, headers);

        TransferResult result = transferLayer.put(repositoryURL + API_V_2_OBJECTS + "/" + objectID, contentType,
            objectContent, headers, parameters);
        int i = result.statusCode();

        if (!(i == 200 || i == 201 || i == 204)) {
            throw new RuntimeException("Error while putting object: " + result.statusMessage());
        }

        try (InputStream inputStream = result.inputStream()) {
            inputStream.readAllBytes();
        }
        return result.headers().get("Location");
    }

    public String putObjectMetadata(String repositoryURL,
                                    String objectID,
                                    String authenticate,
                                    ByteArrayInputStream objectContent,
                                    String contentType) throws URISyntaxException, IOException {
        HashMap<String, List<String>> parameters = new HashMap<>();
        HashMap<String, String> headers = new HashMap<>();

        applyAuth(authenticate, headers);
        adaptRequestParameters(parameters, headers);

        TransferResult result = transferLayer.put(repositoryURL + API_V_2_OBJECTS + "/" + objectID + "/metadata", contentType,
                objectContent, headers, parameters);
        int i = result.statusCode();

        if (!(i == 200 || i == 201 || i == 204)) {
            throw new RuntimeException("Error while putting object: " + result.statusMessage());
        }

        try (InputStream inputStream = result.inputStream()) {
            inputStream.readAllBytes();
        }
        return result.headers().get("Location");

    }

    private void applyQueryParameters(MyCoReObjectQuery query, HashMap<String, List<String>> parameters) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"));

        if (query.getAfterId() != null) {
            parameters.put("after_id", List.of(query.getAfterId()));
        }
        if (query.getCreatedBefore() != null) {
            parameters.put("created_before", List.of(formatter.format(query.getCreatedBefore())));
        }
        if (query.getCreatedAfter() != null) {
            parameters.put("created_after", List.of(formatter.format(query.getCreatedAfter())));
        }
        if(query.getCreatedBy() != null) {
            parameters.put("created_by", List.of(query.getCreatedBy()));
        }
        if (query.getDeletedBefore() != null) {
            parameters.put("deleted_before", List.of(formatter.format(query.getDeletedBefore())));
        }
        if (query.getDeletedAfter() != null) {
            parameters.put("deleted_after", List.of(formatter.format(query.getDeletedAfter())));
        }
        if (query.getDeletedBy() != null) {
            parameters.put("deleted_by", List.of(query.getDeletedBy()));
        }
        if (query.getIncludeCategories() != null) {
            parameters.put("include_categories", query.getIncludeCategories());
        }
        if (query.getLimit() != -1) {
            parameters.put("limit", List.of(String.valueOf(query.getLimit())));
        }
        if (query.getModifiedBefore() != null) {
            parameters.put("modified_before", List.of(query.getModifiedBefore().toString()));
        }
        if (query.getModifiedAfter() != null) {
            parameters.put("modified_after", List.of(query.getModifiedAfter().toString()));
        }
        if (query.getModifiedBy() != null) {
            parameters.put("modified_by", List.of(query.getModifiedBy()));
        }
        if (query.getNumberGreater() != -1) {
            parameters.put("number_greater", List.of(String.valueOf(query.getNumberGreater())));
        }
        if (query.getNumberLess() != -1) {
            parameters.put("number_less", List.of(String.valueOf(query.getNumberLess())));
        }
        if (query.getOffset() != -1) {
            parameters.put("offset", List.of(String.valueOf(query.getOffset())));
        }
        if (query.getProject() != null) {
            parameters.put("project", List.of(query.getProject()));
        }
        if (query.getSortBy() != null) {
            parameters.put("sort_by", List.of(query.getSortBy().name()));
        }
        if (query.getSortOrder() != null) {
            parameters.put("sort_order", List.of(query.getSortOrder().name()));
        }
        if (query.getStatus() != null) {
            parameters.put("status", List.of(query.getStatus()));
        }
        if (query.getType() != null) {
            parameters.put("type", List.of(query.getType()));
        }

    }



}
