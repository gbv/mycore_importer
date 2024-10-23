package de.vzg.oai_importer.mycore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

class MODSUtilTest {

    @Test
    void testParseIdentifierFromJsonString() {
        String jsonString = "{\"identifier\": \"test-id\"}";
        String result = MODSUtil.parseIdentifierFromJsonString(jsonString);
        assertEquals("test-id", result);
    }


    @Test
    void testGetRegisteredIdentifier() throws JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test_mods_1.xml")) {
            Document document = saxBuilder.build(inputStream);


            // Call the method to test
            List<Element> registeredIdentifiers = MODSUtil.getRegisteredIdentifier(document);

            // Verify the output
            assertEquals(2, registeredIdentifiers.size());
            assertEquals("10.0000/test-doi-00000001", registeredIdentifiers.get(0).getText());
            assertEquals("urn:test:0000-0000-0000-0001", registeredIdentifiers.get(1).getText());
        }
    }

    @Test
    void testInsertIdentifiers() throws JDOMException, IOException {
        // Load the XML file
        SAXBuilder saxBuilder = new SAXBuilder();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test_mods_2.xml")) {
            Document document = saxBuilder.build(inputStream);

            // Create identifiers to insert
            Element identifier1 = new Element("identifier", MODSUtil.MODS_NAMESPACE);
            identifier1.setText("10.0000/test-doi-00000002");
            identifier1.setAttribute("type", "doi");

            Element identifier2 = new Element("identifier", MODSUtil.MODS_NAMESPACE);
            identifier2.setText("urn:test:0000-0000-0000-0002");
            identifier2.setAttribute("type", "urn");

            List<Element> identifiers = List.of(identifier1, identifier2);

            // Call the method to test
            MODSUtil.insertIdentifiers(document, identifiers);

            // Verify the output
            List<Element> insertedIdentifiers = document.getRootElement()
                    .getChild("metadata")
                    .getChild("def.modsContainer")
                    .getChild("modsContainer")
                    .getChild("mods", MODSUtil.MODS_NAMESPACE)
                    .getChildren("identifier", MODSUtil.MODS_NAMESPACE);

            assertEquals(2, insertedIdentifiers.size());
            assertTrue(insertedIdentifiers.stream().anyMatch(e -> e.getText().equals("10.0000/test-doi-00000002")));
            assertTrue(insertedIdentifiers.stream().anyMatch(e -> e.getText().equals("urn:test:0000-0000-0000-0002")));
        }

    }
}
