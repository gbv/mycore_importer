package de.vzg.oai_importer.mycore.api.transfer;

import de.vzg.oai_importer.mycore.api.model.MyCoReObjectList;

import java.io.IOException;
import java.io.InputStream;

public interface ResultMapper<T> {
    T mapGeneric(InputStream is) throws IOException;

    MyCoReObjectList mapObjectList(InputStream is) throws IOException;

}
