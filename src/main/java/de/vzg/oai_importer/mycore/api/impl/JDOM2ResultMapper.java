package de.vzg.oai_importer.mycore.api.impl;

import java.io.IOException;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.vzg.oai_importer.mycore.api.model.MyCoReFileListDirectory;
import de.vzg.oai_importer.mycore.api.model.MyCoReObjectList;
import de.vzg.oai_importer.mycore.api.transfer.ResultMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

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

    @Override
    public MyCoReFileListDirectory mapFileList(InputStream is) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(MyCoReFileListDirectory.class);
            return (MyCoReFileListDirectory) context.createUnmarshaller().unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
}
