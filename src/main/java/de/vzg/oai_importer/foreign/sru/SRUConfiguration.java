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

package de.vzg.oai_importer.foreign.sru;

import de.vzg.oai_importer.foreign.Configuration;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SRUConfiguration implements Configuration {

    private String queryPattern;

    private String url;

    private LocalDate oldestDate;

    private LocalDate dateOverwrite;

    private LocalDate newestDate;

    private Integer dayOffset = 1;

    private String recordFilterService;

    @Override
    public String getName() {
        return "SRU: " + queryPattern;
    }

    @Override
    public String getHarvester() {
        return SRUHarvester.SRU_HARVESTER;
    }

}
