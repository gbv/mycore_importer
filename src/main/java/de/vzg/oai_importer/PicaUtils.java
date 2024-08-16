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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

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

    // TODO: Datum wann der gesamte Datensatz zuletzt geändert wurde, eventuell wäre es besser das Datum aus dem
    //  richtigen exemplar zu nehmen
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

    public static List<OffsetDateTime> getCreatedDate(Element rootElement) {
        return rootElement.getChildren("datafield", Namespaces.PICA_NAMESPACE)
            .stream()
            .filter(e -> e.getAttributeValue("tag").equals("001A"))
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

    /**
     * Returns a list of subrecords. A subrecord is a list of elements that belong together, starting with a datafield
     * with tag 101@ and ending just before the next datafield with tag 101@.
     *
     * @param rootElement the record to get the subrecords from
     * @return a list of subrecords each containing a list of elements
     */
    public static List<List<Element>> getSubRecords(Element rootElement) {
        List<List<Element>> subrecords = new ArrayList<>();

        ArrayList<Element> currentSubRecord = null;
        for (Element datafield : rootElement.getChildren("datafield", Namespaces.PICA_NAMESPACE)) {
            // this means a new subrecord starts
            Attribute attribute = datafield.getAttribute("tag");
            if (attribute != null) {
                String tag = attribute.getValue();
                if ("101@".equals(tag)) {
                    if (currentSubRecord != null) {
                        // add the previous subrecord
                        subrecords.add(currentSubRecord);
                    }
                    currentSubRecord = new ArrayList<>();
                }
            }

            if (currentSubRecord != null) {
                currentSubRecord.add(datafield);
            }
        }
        if (currentSubRecord != null) {
            subrecords.add(currentSubRecord);
        }
        return subrecords;
    }


    /**
     * Checks if the given subrecord contains all required tag code values.
     * @param subRecordChildren the subrecord to check
     * @param required the required tag code values
     * @return true if the subrecord contains all required tag code values
     */
    public static boolean matchingSubRecord(List<Element> subRecordChildren, List<TagCodeValue> required) {
        Predicate<TagCodeValue> tagCodeMatching = currentRequiredTagCode -> {
            for (Element subRecordChild : subRecordChildren) {
                String tag = subRecordChild.getAttributeValue("tag");
                if (tag == null) {
                    continue;
                }

                for (Element subfield : subRecordChild.getChildren("subfield", Namespaces.PICA_NAMESPACE)) {
                    String code = subfield.getAttributeValue("code");
                    String value = subfield.getText();
                    if (currentRequiredTagCode.code.equals(code) && currentRequiredTagCode.value.equalsIgnoreCase(value)) {
                        return true;
                    }
                }
            }
            return false;
        };

       return required.stream().allMatch(tagCodeMatching);
    }

    public record TagCodeValue(String tag, String code, String value) {
    }
}
