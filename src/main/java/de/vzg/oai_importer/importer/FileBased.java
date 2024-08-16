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

package de.vzg.oai_importer.importer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;

public interface FileBased {

    List<String> listImportableFiles(MyCoReTargetConfiguration target, ForeignEntity record)
        throws IOException, URISyntaxException;

    List<String> listMissingFiles(MyCoReTargetConfiguration target, MyCoReObjectInfo info, ForeignEntity record)
        throws IOException, URISyntaxException;

    List<String> fixMissingFiles(MyCoReTargetConfiguration target, MyCoReObjectInfo info, ForeignEntity record)
        throws IOException, URISyntaxException;

}
