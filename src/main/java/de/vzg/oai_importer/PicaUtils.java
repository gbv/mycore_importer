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

package de.vzg.oai_importer;

import org.jdom2.Document;
import org.jdom2.Element;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class PicaUtils {

    public static Stream<String> getPicaField(Document root, String tag, String code) {
        return root.getRootElement().getChildren("datafield", Namespaces.PICA_NAMESPACE)
                .stream()
                .filter(e -> e.getAttributeValue("tag").equals(tag))
                .map(element -> element.getChildren().stream().filter(e -> e.getAttributeValue("code").equals(code))
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Element::getText);
    }

    public static List<OffsetDateTime> getModifiedDate(Element rootElement) {
        return rootElement.getChildren("datafield", Namespaces.PICA_NAMESPACE)
                .stream()
                .filter(e -> e.getAttributeValue("tag").equals("001B"))
                .map(element -> {
                    String p0
                            = element.getChildren().stream().filter(e -> e.getAttributeValue("code").equals("0"))
                            .findFirst().get().getText();

                    String time
                            = element.getChildren().stream().filter(e -> e.getAttributeValue("code").equals("t"))
                            .findFirst().get().getText();

                    String date = p0.split(":")[1];

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss.SSS");
                    OffsetDateTime dateTime = LocalDateTime.parse(date + " " + time, formatter)
                            .atOffset(OffsetDateTime.now().getOffset());
                    return dateTime;
                }).toList();
    }

}
