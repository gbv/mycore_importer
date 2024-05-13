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

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import lombok.extern.log4j.Log4j2;

@Log4j2
class SRUHarvesterTest {

    @Test
    void buildLink() {
        SRUHarvester harvester = new SRUHarvester();

        SRUConfiguration sourceConfiguration = new SRUConfiguration();

        sourceConfiguration.setUrl("http://example.com");
        sourceConfiguration.setQueryPattern("date={date}");

        LocalDate nowLocalDate = LocalDate.now();
        String link = harvester.buildLink(sourceConfiguration, nowLocalDate, 1, 10);

        Assert.notNull(link, "Link should not be null");

        Assert.isTrue(link.contains("http://example.com"), "Link should contain the base url");
        Assert.isTrue(link.contains("date%3D" + nowLocalDate.toString()), "Link should contain the date");

        Assert.isTrue(link.contains("startRecord=1"), "Link should contain the startRecord");
        Assert.isTrue(link.contains("maximumRecords=10"), "Link should contain the maximumRecords");

        log.info("Link: " + link);
    }

    @Test
    void getDaysSince() {
    }
}