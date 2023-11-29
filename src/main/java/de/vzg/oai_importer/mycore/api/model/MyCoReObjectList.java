package de.vzg.oai_importer.mycore.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mycoreobjects")
@JsonSerialize(using = MyCoReObjectListSerializer.class)
@JsonDeserialize(using = MyCoReObjectListDeserializer.class)
public class MyCoReObjectList {

    private List<MyCoReObjectListEntry> entries = new ArrayList<>();

    @XmlElement(name = "mycoreobject", required = false)
    public List<MyCoReObjectListEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<MyCoReObjectListEntry> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        return "MyCoReObjectList{" +
                "entries=" + entries +
                '}';
    }
}
