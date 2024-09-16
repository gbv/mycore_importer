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

import java.util.List;

import org.jdom2.Element;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.PicaUtils;
import de.vzg.oai_importer.importer.FileRightsDetector;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service("DFIFileRightsDetector")
public class DFIFileRightsDetector implements FileRightsDetector {



    @Override
    public boolean isPublic(Element record) {


        for (List<Element> subRecord : PicaUtils.getSubRecords(record)) {
            if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615))){


                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_DFI_AKTUELL))){
                    log.info("DFI Aktuell found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_AFA))){
                    log.info("AFA found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_DFI_COMPACT))){
                    log.info("DFI Compact found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_TONDOKUMENT))){
                    log.info("Tondokument found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_VIDEO))){
                    log.info("Video found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_ABSCHLUSSARBEIT))){
                    log.info("Abschlussarbeit found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_ZEITSCHRIFTENAUFSATZ))){
                    log.info("Zeitschriftenaufsatz found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_SICHERHEITSKOPIE))){
                    log.info("Sicherheitskopie found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_PRESSEARTIKEL))){
                    log.info("Presseartikel found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_KARIKATUR))){
                    log.info("Karikatur found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_PA_VOLLLTEXT))){
                    log.info("PA-Volltext found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_GFFK))) {
                    log.info("GFfK found");
                    return true;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_615_KOMMENTAR_PRESSEMAPPE))) {
                    log.info("Pressemappe found");
                    return false;
                }
            }


            if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3))) {
                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_SICHERHEITSKOPIE))){
                    log.info("Sicherheitskopie found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_PRESSEARTIKEL))){
                    log.info("Presseartikel found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_KARIKATUR))){
                    log.info("Karikatur found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_PA_VOLLLTEXT))){
                    log.info("PA-Volltext found");
                    return false;
                }

                if(PicaUtils.matchingSubRecord(subRecord, List.of(SIEGEL_LG3_PRESSEMAPPE))) {
                    log.info("Pressemappe found");
                    return false;
                }
            }

        }


        return false;

    }
}
