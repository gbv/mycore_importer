package de.vzg.oai_importer.mycore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import de.vzg.oai_importer.mycore.api.MCRRestCredentials;
import de.vzg.oai_importer.mycore.api.MyCoReObjectQuery;
import de.vzg.oai_importer.mycore.api.impl.ApacheHttpClientTransferLayer;
import de.vzg.oai_importer.mycore.api.impl.MyCoReV2JDOMClient;
import de.vzg.oai_importer.mycore.api.model.MyCoReObjectList;

@Service
public class MyCoReRestAPIService {

    ConcurrentHashMap<MyCoReTargetConfiguration, TokenValidation> targetTokenMap = new ConcurrentHashMap<>();

    public UserInfo authenticate(MyCoReTargetConfiguration target, String username, String password) throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());
        String authenticate = client.authenticate(url, new MCRRestCredentials(username, password));

        // parse authenticate with regexp
        String token = authenticate.split(" ")[1];
        DecodedJWT decodedJWT = JWT.decode(token);
        List<String> roles = decodedJWT.getClaim("mcr:roles").asList(String.class);
        String id = decodedJWT.getSubject();
        String name = decodedJWT.getClaim("name").asString();
        return new UserInfo(id, name, roles);
    }

    public String authenticate(MyCoReTargetConfiguration target) throws IOException, URISyntaxException {
        String user = target.getUser();
        String password = target.getPassword();

        if (user == null || password == null) {
            return null;
        }

        if (targetTokenMap.containsKey(target)) {
            TokenValidation tokenValidation = targetTokenMap.get(target);
            if (tokenValidation.expires.isAfter(Instant.now())) {
                return tokenValidation.token;
            } else {
                targetTokenMap.remove(target);
            }
        }

        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());
        String authenticate = client.authenticate(url, new MCRRestCredentials(user, password));

        if (authenticate != null) {
            targetTokenMap.put(target, new TokenValidation(authenticate, Instant.now().plusSeconds(10 * 60)));
        }

        return authenticate;
    }

    public MyCoReObjectList getObjects(MyCoReTargetConfiguration target, MyCoReObjectQuery query)
        throws IOException, URISyntaxException {

        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        MyCoReObjectList list;
        if (authenticate != null) {
            list = client.listObjects(url, query, authenticate);
        } else {
            list = client.listObjects(url, query);
        }

        return list;
    }

    public Document getObject(MyCoReTargetConfiguration target, String objectID)
        throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);

        Document object;
        if (authenticate != null) {
            object = client.getObject(url, objectID, authenticate);
        } else {
            object = client.getObject(url, objectID);
        }

        return object;
    }

    public Document getDerivate(MyCoReTargetConfiguration target, String objectID, String derivateID) throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());
        String authenticate = authenticate(target);
        return client.getDerivate(url, objectID, derivateID, authenticate);
    }

    public String postObject(MyCoReTargetConfiguration target, Document object) throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        if (authenticate == null) {
            throw new IOException("Could not authenticate");
        }
        byte[] byteArray;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new XMLOutputter().output(object, out);
            byteArray = out.toByteArray();
        }
        try (ByteArrayInputStream out = new ByteArrayInputStream(byteArray)) {
            return client.postObject(url,  authenticate, out, "application/xml");
        }
    }

    public String postDerivate(MyCoReTargetConfiguration target, String object, String order, String maindoc, List<String> classifications, List<String> titles) throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        if (authenticate == null) {
            throw new IOException("Could not authenticate");
        }

        return client.postDerivate(url, object,  authenticate, order, maindoc, classifications, titles );
    }

    public String putDerivate(MyCoReTargetConfiguration target, String objectID, String derivateID, Document object) throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        if (authenticate == null) {
            throw new IOException("Could not authenticate");
        }
        byte[] byteArray;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new XMLOutputter().output(object, out);
            byteArray = out.toByteArray();
        }
        try (ByteArrayInputStream out = new ByteArrayInputStream(byteArray)) {
            return client.putDerivate(url, objectID, derivateID, authenticate, out, "application/xml");
        }
    }

    public String putObject(MyCoReTargetConfiguration target, String objectID, Document object)
        throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        if (authenticate == null) {
            throw new IOException("Could not authenticate");
        }
        byte[] byteArray;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new XMLOutputter().output(object, out);
            byteArray = out.toByteArray();
        }
        try (ByteArrayInputStream out = new ByteArrayInputStream(byteArray)) {
           return client.putObject(url, objectID, authenticate, out, "application/xml");
        }
    }

    public String putObjectMetadata(MyCoReTargetConfiguration target, String objectID, Document object)
        throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        if (authenticate == null) {
            throw new IOException("Could not authenticate");
        }
        byte[] byteArray;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new XMLOutputter().output(object, out);
            byteArray = out.toByteArray();
        }
        try (ByteArrayInputStream out = new ByteArrayInputStream(byteArray)) {
           return client.putObjectMetadata(url, objectID, authenticate, out, "application/xml");
        }
    }

    public void putFiles(MyCoReTargetConfiguration target, String objectID, String derivativeID, String filename, InputStream is)
        throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        String authenticate = authenticate(target);
        if (authenticate == null) {
            throw new IOException("Could not authenticate");
        }
        client.putFile(url, objectID, derivativeID, authenticate, "/"+filename, is);
    }

    public List<Category> getClassificationCategories(MyCoReTargetConfiguration target, String classId)
        throws IOException, URISyntaxException {
        String url = target.getUrl();
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());

        Document classification = client.getClassification(url, classId);
        XPathExpression<Element> categoryXPath = XPathFactory.instance().compile(".//category", Filters.element());
        List<Element> categoryElements = categoryXPath.evaluate(classification);

        return categoryElements.stream()
            .map(element -> {
                String id = element.getAttributeValue("ID");
                List<Element> children = element.getChildren("label");
                String label = children.stream()
                    .filter(l -> l.getAttributeValue("lang", Namespace.XML_NAMESPACE).equals("de"))
                    .findFirst().map(l -> l.getAttributeValue("text"))
                    .or(() -> children.stream().findFirst().map(l -> l.getAttributeValue("text")))
                    .orElseGet(() -> id);
                return new Category(classId, id, label);
            }).toList();
    }

    record TokenValidation(String token, Instant expires) {
    }

    public record Category(String classId, String id, String label) {
    }

    public record UserInfo(String id, String name, List<String> roles) {
    }
}
