/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vzg.oai_importer.mycore.api.model;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "directory")
public class MyCoReFileListDirectory {

    private Instant lastModified;

    private String name;

    private List<MyCoReFileListDirectory> directories;

    private List<MyCoReFileListFile> files;

    @XmlElement(name = "directory", required = false)
    public List<MyCoReFileListDirectory> getDirectories() {
        return directories;
    }

    public void setDirectories(List<MyCoReFileListDirectory> directories) {
        this.directories = directories;
    }

    @XmlElement(name = "file", required = false)
    public List<MyCoReFileListFile> getFiles() {
        return files;
    }

    public void setFiles(List<MyCoReFileListFile> files) {
        this.files = files;
    }

    @XmlAttribute(name = "lastModified", required = true)
    @XmlJavaTypeAdapter(value = MyCoReInstantXMLAdapter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    @XmlAttribute(name = "name", required = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    
}
