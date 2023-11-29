package de.vzg.oai_importer.mycore.api.impl;

import de.vzg.oai_importer.mycore.api.transfer.ResultMapper;
import de.vzg.oai_importer.mycore.api.model.MyCoReObjectList;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;

public class JDOM2ResultMapper implements ResultMapper<Document> {

    @Override
    public Document mapGeneric(InputStream is) throws IOException {
        try {
            return new SAXBuilder().build(is);
        } catch (JDOMException e) {
            throw new IOException(e);
        }
    }

    @Override
    public MyCoReObjectList mapObjectList(InputStream is) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(MyCoReObjectList.class);
            return (MyCoReObjectList) context.createUnmarshaller().unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
}
