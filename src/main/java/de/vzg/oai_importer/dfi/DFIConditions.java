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

import de.vzg.oai_importer.PicaUtils;

public class DFIConditions {
    public static final PicaUtils.TagCodeValue SIEGEL_615 = new PicaUtils.TagCodeValue("209A", "B", "615");
    public static final PicaUtils.TagCodeValue SIEGEL_LG3 = new PicaUtils.TagCodeValue("209A", "B", "Lg 3");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_DFI_AKTUELL
            = new PicaUtils.TagCodeValue("237A", "a", "dfi aktuell");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_AFA
            = new PicaUtils.TagCodeValue("237A", "a", "afa");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_DFI_COMPACT
            = new PicaUtils.TagCodeValue("237A", "a", "dfi compact");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_TONDOKUMENT
            = new PicaUtils.TagCodeValue("237A", "a", "tondokument");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_VIDEO
            = new PicaUtils.TagCodeValue("237A", "a", "video");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_ABSCHLUSSARBEIT
            = new PicaUtils.TagCodeValue("237A", "a", "abschlussarbeit");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_ZEITSCHRIFTENAUFSATZ
            = new PicaUtils.TagCodeValue("237A", "a", "Zeitschriftenaufsatz");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_SICHERHEITSKOPIE
            = new PicaUtils.TagCodeValue("237A", "a", "Sicherheitskopie");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_PRESSEARTIKEL
            = new PicaUtils.TagCodeValue("237A", "a", "Presseartikel");


    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_KARIKATUR
            = new PicaUtils.TagCodeValue("237A", "a", "Karikatur");


    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_PA_VOLLLTEXT
            = new PicaUtils.TagCodeValue("237A", "a", "PA-Volltext");


    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_GFFK
            = new PicaUtils.TagCodeValue("237A", "a", "GFfK");

    public static final PicaUtils.TagCodeValue SIEGEL_615_KOMMENTAR_PRESSEMAPPE
            = new PicaUtils.TagCodeValue("237A", "a", "Pressemappe");


    public static final PicaUtils.TagCodeValue SIEGEL_LG3_SICHERHEITSKOPIE
            = new PicaUtils.TagCodeValue("209A", "a", "Sicherheitskopie");

    public static final PicaUtils.TagCodeValue SIEGEL_LG3_PRESSEARTIKEL
            = new PicaUtils.TagCodeValue("209A", "a", "Presseartikel");

    public static final PicaUtils.TagCodeValue SIEGEL_LG3_KARIKATUR
            = new PicaUtils.TagCodeValue("209A", "a", "Karikatur");

    public static final PicaUtils.TagCodeValue SIEGEL_LG3_PA_VOLLLTEXT
            = new PicaUtils.TagCodeValue("209A", "a", "PA-Volltext");

    public static final PicaUtils.TagCodeValue SIEGEL_LG3_PRESSEMAPPE
            = new PicaUtils.TagCodeValue("209A", "a", "Pressemappe");
}
