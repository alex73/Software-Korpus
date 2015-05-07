/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013-2015 Aleś Bułojčyk (alex73mail@gmail.com)
               Home page: https://sourceforge.net/projects/korpus/

 This file is part of Korpus.

 Korpus is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Korpus is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.korpus.client;

import org.alex73.korpus.shared.dto.SearchResults;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class DocumentDetailsPanel extends VerticalPanel {
    public DocumentDetailsPanel(SearchResults s, Korpus screen) {
        String html = "";

        html += "<i><u>";
        if (s.doc != null) {
            switch (s.doc.authors.length) {
            case 0:
                break;
            case 1:
                html += "Аўтар: " + s.doc.authors[0] + "<br/>";
                break;
            default:
                html += "Аўтары: ";
                for (int i = 0; i < s.doc.authors.length; i++) {
                    if (i > 0) {
                        html += ",";
                    }
                    html += s.doc.authors[i];
                }
                html += "<br/>";
                break;
            }
            html += "Назва: " + s.doc.title;
            if (s.doc.writtenYear != null) {
                html += "<br/>Год напісаньня: " + s.doc.writtenYear;
            }
            if (s.doc.publishedYear != null) {
                html += "<br/>Год выданьня: " + s.doc.publishedYear;
            }
        } else if (s.docOther != null) {
            html += s.docOther.textURL;
        }
        html += "</u></i>";

        add(new HTMLPanel(html));
        add(createWords(s, screen));
    }

    HTMLPanel createWords(SearchResults s, Korpus screen) {
        HTMLPanel p = new HTMLPanel("");
        ResultsSearch.outputText(s, p, screen, false, 0, 3," <...> ");
        return p;
    }
}
