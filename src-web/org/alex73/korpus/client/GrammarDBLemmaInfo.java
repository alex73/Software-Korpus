/**************************************************************************
 Korpus - Corpus Linguistics Software.

 Copyright (C) 2013 Aleś Bułojčyk (alex73mail@gmail.com)
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

import java.util.List;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.shared.LemmaInfo;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Show details of paradigm.
 */
public class GrammarDBLemmaInfo extends VerticalPanel {
    public GrammarDBLemmaInfo(LemmaInfo[] lemmas, GrammarDB screen) {
        for (LemmaInfo li : lemmas) {
            List<String> tags = BelarusianTags.getInstance().describe(li.lemmaGrammar);
            add(new Label("Лема: " + li.lemma + " " + tags));
            add(showForms(li.words, tags.size()));
            add(new Label(" "));
        }
    }

    HTMLPanel showForms(LemmaInfo.Word[] forms, int skipTagsCount) {
        int maxTagCount = 0;
        for (LemmaInfo.Word f : forms) {
            List<String> tags = BelarusianTags.getInstance().describe(f.cat);
            maxTagCount = Math.max(maxTagCount, tags.size());
        }
        String[][] tags = new String[forms.length][];
        int[][] rowspans = new int[forms.length][];
        for (int i = 0; i < forms.length; i++) {
            tags[i] = new String[maxTagCount + 1];
            for (int j = 0; j < tags[i].length; j++) {
                tags[i][j] = "";
            }
            rowspans[i] = new int[maxTagCount + 1];
            tags[i][maxTagCount] = forms[i].value;
            List<String> t = BelarusianTags.getInstance().describe(forms[i].cat);
            for (int j = 0; j < t.size(); j++) {
                tags[i][j] = t.get(j);
            }
        }
        for (int i = 0; i < tags.length; i++) {
            for (int j = 0; j < tags[i].length; j++) {
                rowspans[i][j] = 1;
            }
        }
        for (int i = forms.length - 1; i > 0; i--) {
            for (int j = 0; j < maxTagCount; j++) {
                if (tags[i][j] == null || tags[i - 1][j] == null) {
                    continue;
                }
                if (tags[i][j].equals(tags[i - 1][j])) {
                    rowspans[i - 1][j] += rowspans[i][j];
                    rowspans[i][j] = 0;
                    tags[i][j] = null;
                }
            }
        }

        String table = "";
        table += "<table border=\"1\">";
        for (int i = 0; i < tags.length; i++) {
            table += "<tr>";
            for (int j = skipTagsCount; j < tags[i].length; j++) {
                if (rowspans[i][j] == 1) {
                    table += "<td>" + tags[i][j] + "</td>";
                } else if (rowspans[i][j] > 1) {
                    table += "<td rowspan=\"" + rowspans[i][j] + "\">" + tags[i][j] + "</td>";
                }
            }
            table += "</tr>";
        }
        table += "</table>";

        return new HTMLPanel(table);
    }
}
