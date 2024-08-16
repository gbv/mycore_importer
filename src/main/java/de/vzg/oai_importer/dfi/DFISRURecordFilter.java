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

package de.vzg.oai_importer.dfi;

import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_ABSCHLUSSARBEIT;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_AFA;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_DFI_AKTUELL;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_DFI_COMPACT;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_GFFK;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_KARIKATUR;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_PA_VOLLLTEXT;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_PRESSEARTIKEL;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_PRESSEMAPPE;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_SICHERHEITSKOPIE;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_TONDOKUMENT;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_VIDEO;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_615_KOMMENTAR_ZEITSCHRIFTENAUFSATZ;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_LG3;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_LG3_KARIKATUR;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_LG3_PA_VOLLLTEXT;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_LG3_PRESSEARTIKEL;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_LG3_PRESSEMAPPE;
import static de.vzg.oai_importer.dfi.DFIConditions.SIEGEL_LG3_SICHERHEITSKOPIE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.PicaUtils;
import de.vzg.oai_importer.foreign.sru.SRURecordFilter;
import lombok.extern.java.Log;

@Service("DFISRURecordFilter")
@Log
public class DFISRURecordFilter implements SRURecordFilter {



    @Override
    public boolean filter(Document record, LocalDate day) {
        Element recordRoot = record.getRootElement();




        for (List<Element> subRecord : PicaUtils.getSubRecords(recordRoot)) {
            List<PicaUtils.TagCodeValue> required = new java.util.ArrayList<>();
            required.add(SIEGEL_615);
            if(day != null) {
                PicaUtils.TagCodeValue date = new PicaUtils.TagCodeValue("201B", "0",
                        day.format(DateTimeFormatter.ofPattern("dd-MM-yy")));
                required.add(date);

            }
            if(PicaUtils.matchingSubRecord(subRecord, required)){


                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_DFI_AKTUELL))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_AFA))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_DFI_COMPACT))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_TONDOKUMENT))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_VIDEO))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_ABSCHLUSSARBEIT))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_ZEITSCHRIFTENAUFSATZ))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_SICHERHEITSKOPIE))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_PRESSEARTIKEL))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_KARIKATUR))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_PA_VOLLLTEXT))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_GFFK))) {
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_PRESSEMAPPE))) {
                    return true;
                }
            }

            required = new java.util.ArrayList<>();
            required.add(SIEGEL_LG3);
            if(day != null) {
                PicaUtils.TagCodeValue date = new PicaUtils.TagCodeValue("201B", "0",
                        day.format(DateTimeFormatter.ofPattern("dd-MM-yy")));
                required.add(date);

            }
            if(PicaUtils.matchingSubRecord(subRecord, required)) {
                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_SICHERHEITSKOPIE))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_PRESSEARTIKEL))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_KARIKATUR))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_PA_VOLLLTEXT))){
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_PRESSEMAPPE))) {
                    return true;
                }
            }

        }


        return false;
    }
}
