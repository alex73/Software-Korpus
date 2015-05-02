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

package org.alex73.korpus.server;

import java.util.List;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.shared.dto.ResultText;
import org.alex73.korpus.shared.dto.SearchParams;
import org.alex73.korpus.shared.dto.WordRequest;
import org.alex73.korpus.shared.dto.WordResult;

/**
 * Some methods for final checks.
 * 
 * Lucene can't process some complex criteria of search(like order of words). This methods used for these
 * complex check for filtering of results from Lucene's search.
 */
public class WordsDetailsChecks {

    /**
     * Is the document correspond with search criteria ? Check and mark requested words for highlight for
     * user.
     */
    public static boolean isAllowed(SearchParams.WordsOrder wordsOrder, List<WordRequest> words,
            ResultText resultText) {
        boolean found = false;
        switch (wordsOrder) {
        case PRESET:
            for (int i = 0; i < resultText.words.length; i++) {
                for (int j = 0; j < resultText.words[i].length; j++) {
                    if (isWordMatchsParamsAround(words, 0, resultText.words[i], j)) {
                        for (int k = 0; k < words.size(); k++) { // mark found words as requested
                            resultText.words[i][j + k].requestedWord = true;
                        }
                        found = true;
                    }
                }
            }
            break;
        case ANY_IN_SENTENCE:
            for (int i = 0; i < resultText.words.length; i++) {
                int c = 0;
                for (WordRequest pw : words) {
                    boolean foundWord = false;
                    for (int j = 0; j < resultText.words[i].length; j++) {
                        if (isWordMatchsParam(pw, resultText.words[i][j])) {
                            resultText.words[i][j].requestedWord = true;
                            foundWord = true;
                        }
                    }
                    if (foundWord) {
                        c++;
                    }
                }
                if (c == words.size()) { // allowed only if all words exist
                    found = true;
                }
            }
            break;
        case ANY_IN_PARAGRAPH:
            int c = 0;
            for (WordRequest pw : words) {
                boolean foundWord = false;
                for (int i = 0; i < resultText.words.length; i++) {
                    for (int j = 0; j < resultText.words[i].length; j++) {
                        if (isWordMatchsParam(pw, resultText.words[i][j])) {
                            resultText.words[i][j].requestedWord = true;
                            foundWord = true;
                        }
                    }
                }
                if (foundWord) {
                    c++;
                }
            }
            found = c == words.size(); // allowed only if all words exist
            break;
        }

        return found;
    }

    /**
     * For the wordsOrder=PRESET: specified words should correspond with specified parameter and all other
     * parameters.
     */
    private static boolean isWordMatchsParamsAround(List<WordRequest> words, int paramIndex,
            WordResult[] resultWords, int wordIndex) {
        int startWord = wordIndex - paramIndex;
        if (startWord < 0) {
            return false;
        }
        int endWord = words.size() - paramIndex + wordIndex;
        if (endWord > resultWords.length) {
            return false;
        }
        for (int i = 0; i < words.size(); i++) {
            if (!isWordMatchsParam(words.get(i), resultWords[startWord + i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is the word corresponds with parameter ?
     */
    public static boolean isWordMatchsParam(WordRequest wordParam, WordResult wordResult) {
        if (wordParam.word != null && !wordParam.word.trim().isEmpty()) {
            if (wordParam.allForms) {
                // lemma
                if (!wordParam.lemmas.contains(wordResult.lemma)) {
                    return false;
                }
            } else {
                // concrete form
                if (!wordParam.word.equalsIgnoreCase(wordResult.value)) {
                    return false;
                }
            }
        }
        if (wordParam.grammar == null) {
            return true;
        }
        if (wordResult.cat == null) {
            return false;
        }
        // check grammar
        for (String t : wordResult.cat.split("_")) {
            if (BelarusianTags.getInstance().isValid(t, null)) {
                try {
                    String findTag = DBTagsGroups.getDBTagString(t);
                    if (findTag.matches(wordParam.grammar)) {
                        return true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return false;
    }
}
