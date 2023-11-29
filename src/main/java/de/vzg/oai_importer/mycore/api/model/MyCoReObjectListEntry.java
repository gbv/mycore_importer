package de.vzg.oai_importer.mycore.api.model;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "mycoreobject")
public final class MyCoReObjectListEntry {

    private String objectID;

    private Instant lastModified;

    MyCoReObjectListEntry() {
    }

    @XmlAttribute(name = "id", required = true)
    /// set jackson name to id
    @JsonProperty("id")
    public String getObjectID() {
        return objectID;
    }

    @XmlAttribute(name="lastModified",required = true)
    @XmlJavaTypeAdapter(value = MyCoReInstantXMLAdapter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public void setObjectID(String objectID) {
        this.objectID = objectID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (MyCoReObjectListEntry) obj;
        return Objects.equals(this.objectID, that.objectID) &&
                Objects.equals(this.lastModified, that.lastModified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectID, lastModified);
    }

    @Override
    public String toString() {
        return "MCRObjectListEntry[" +
                "objectID=" + objectID + ", " +
                "lastModified=" + lastModified + ']';
    }

}
