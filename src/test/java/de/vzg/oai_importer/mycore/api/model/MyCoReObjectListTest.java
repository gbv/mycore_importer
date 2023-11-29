package de.vzg.oai_importer.mycore.api.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

class MyCoReObjectListTest {

    public static final Instant TIME_1 = Instant.now();
    public static final Instant TIME_2 = Instant.now().minus(100, ChronoUnit.DAYS);
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String OBJECT_ID = "objectID";
    public static final String OBJECT_ID_2 = "objectID2";

    @Test
    void testMarshalling() throws JAXBException, IOException, JDOMException {
        MyCoReObjectList actualMCRObjectList =  getTestData();



        JAXBContext context = JAXBContext.newInstance(MyCoReObjectList.class);
        String resultString;

        try(StringWriter writer = new StringWriter()){
            context.createMarshaller().marshal(actualMCRObjectList, writer);
            resultString = writer.toString();
        }

        try(StringReader characterStream = new StringReader(resultString)){
            SAXBuilder saxBuilder = new SAXBuilder();
            Document build = saxBuilder.build(characterStream);
            Element rootElement = build.getDocument().getRootElement();

            Assertions.assertEquals(2, rootElement.getChildren().size());

            Assertions.assertEquals("mycoreobjects", rootElement.getName());

            Element firstElement = rootElement.getChildren().get(0);
            Assertions.assertEquals("mycoreobject", firstElement.getName());
            Assertions.assertEquals(OBJECT_ID, firstElement.getAttributeValue("id"));


            Element secondElement = rootElement.getChildren().get(1);
            Assertions.assertEquals("mycoreobject", secondElement.getName());
            Assertions.assertEquals(OBJECT_ID_2, secondElement.getAttributeValue("id"));
        }

        LOGGER.info(resultString);

        try(StringReader stringReader = new StringReader(resultString)){
            MyCoReObjectList unmarshal = (MyCoReObjectList) context.createUnmarshaller().unmarshal(stringReader);
            List<MyCoReObjectListEntry> entries = unmarshal.getEntries();
            Assertions.assertEquals(2, entries.size());
            Assertions.assertEquals(OBJECT_ID, entries.get(0).getObjectID());
            Assertions.assertEquals(TIME_1, entries.get(0).getLastModified());
            Assertions.assertEquals(OBJECT_ID_2, entries.get(1).getObjectID());
            Assertions.assertEquals(TIME_2, entries.get(1).getLastModified());
        }


    }

    private MyCoReObjectList getTestData() {
        MyCoReObjectList actualMCRObjectList = new MyCoReObjectList();
        MyCoReObjectListEntry entry1 = new MyCoReObjectListEntry();

        entry1.setObjectID(OBJECT_ID);
        entry1.setLastModified(TIME_1);


        MyCoReObjectListEntry entry2 = new MyCoReObjectListEntry();
        entry2.setObjectID(OBJECT_ID_2);
        entry2.setLastModified(TIME_2);


        actualMCRObjectList
                .setEntries(Stream.of(entry1, entry2)
                        .toList());
        return actualMCRObjectList;
    }

    @Test
    void testJSON() throws JsonProcessingException {
        MyCoReObjectList testData = getTestData();

        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();

        mapper.registerModule(module);

        String result = mapper.writeValueAsString(testData);

        MyCoReObjectList mcrObjectList = mapper.readValue(result, MyCoReObjectList.class);
        Assertions.assertEquals(2, mcrObjectList.getEntries().size());
        Assertions.assertEquals(OBJECT_ID, mcrObjectList.getEntries().get(0).getObjectID());
        Assertions.assertEquals(TIME_1, mcrObjectList.getEntries().get(0).getLastModified());
        Assertions.assertEquals(OBJECT_ID_2, mcrObjectList.getEntries().get(1).getObjectID());
        Assertions.assertEquals(TIME_2, mcrObjectList.getEntries().get(1).getLastModified());

    }

}
