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

package org.alex73.korpus.shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters for search corpus documents.
 */
public class SearchParams implements Serializable {
    public enum WordsOrder {
        PRESET, ANY_IN_SENTENCE, ANY_IN_PARAGRAPH
    };

    public CorpusType corpusType;
    public TextStandardInfo textStandard = new TextStandardInfo();
    public TextUnprocessedInfo textUnprocessed = new TextUnprocessedInfo();
    public List<WordRequest> words = new ArrayList<WordRequest>();
    public WordsOrder wordsOrder = WordsOrder.PRESET;

    public boolean isTooSimple() {
        for (WordRequest w : words) {
            if (w.word != null) {
                String wt = w.word.trim();
                if (wt.length() > 1) {
                    return false;
                }
                if (wt.length() == 1 && Character.isLetter(wt.charAt(0))) {
                    return false;
                }

                if (w.grammar != null) {
                    String gt = w.grammar.trim();
                    if (!gt.startsWith("K")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
