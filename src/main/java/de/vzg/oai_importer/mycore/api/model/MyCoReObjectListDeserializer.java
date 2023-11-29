package de.vzg.oai_importer.mycore.api.model;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class MyCoReObjectListDeserializer extends JsonDeserializer<MyCoReObjectList> {
    @Override
    public MyCoReObjectList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if (p.isExpectedStartArrayToken()) {
            List<MyCoReObjectListEntry> entries = p.readValueAs(new TypeReference<List<MyCoReObjectListEntry>>() {
            });
            MyCoReObjectList mcrObjectList = new MyCoReObjectList();
            mcrObjectList.setEntries(entries);
            return mcrObjectList;
        }
        return null;
    }
}
