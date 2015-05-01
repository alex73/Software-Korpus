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

package org.alex73.korpus.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters for search corpus documents.
 */
public class SearchParams implements Serializable {
    public enum CorpusType {
        STANDARD, UNPROCESSED
    };

    public enum WordsOrder {
        PRESET, ANY
    };

    public CorpusType corpusType;
    public TextStandard textStandard = new TextStandard();
    public TextUnprocessed textUnprocessed = new TextUnprocessed();
    public List<Word> words = new ArrayList<Word>();
    public WordsOrder wordsOrder = WordsOrder.PRESET;

    public static class TextStandard implements Serializable {
        public String author;
        public List<String> stylegenres;
        public Integer yearWrittenFrom, yearWrittenTo, yearPublishedFrom, yearPublishedTo;
    }

    public static class TextUnprocessed implements Serializable {
        public String volume;
    }

    public static class Word implements Serializable {
        public String word;
        public boolean allForms;
        public String grammar;
        public List<String> lemmas;
    }

    public boolean isTooSimple() {
        for (Word w : words) {
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
