package de.vzg.oai_importer.mycore.api.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class MyCoReObjectListSerializer extends JsonSerializer<MyCoReObjectList>{

    @Override
    public void serialize(MyCoReObjectList o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(o!=null){
            jsonGenerator.writeObject(o.getEntries());
        }
    }

}
