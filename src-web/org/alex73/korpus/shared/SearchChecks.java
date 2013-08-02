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

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.base.DBTagsGroups;

/**
 * Some methods for final checks.
 * 
 * Lucene can't process soem complex criteria of search(like order of words). In
 * this case, these methods used for additional filtering of results from
 * Lucene's search.
 */
public class SearchChecks {

    /**
     * Is the document correspond with search criteria ?
     */
    public static boolean isFoundDoc(SearchParams params, ResultSentence result) {
        switch (params.wordsOrder) {
        case PRESET:
            for (int i = 0; i < result.words.length; i++) {
                if (isWordMatchsParamsAround(params, 0, result, i)) {
                    return true;
                }
            }
            return false;
        case ANY:
            for (SearchParams.Word pw : params.words) {
                boolean found = false;
                for (ResultSentence.Word rw : result.words) {
                    if (isWordMatchsParam(pw, rw)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Is the word correspond with search criteria ?
     */
    public static boolean isFoundWord(SearchParams params, ResultSentence result, int wordIndex) {
        switch (params.wordsOrder) {
        case PRESET:
            for (int i = 0; i < params.words.size(); i++) {
                if (isWordMatchsParamsAround(params, i, result, wordIndex)) {
                    return true;
                }
            }
            return false;
        case ANY:
            ResultSentence.Word rw = result.words[wordIndex];
            for (SearchParams.Word pw : params.words) {
                if (isWordMatchsParam(pw, rw)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * For the wordsOrder=PRESET: specified words should correspond with
     * specified parameter and all other parameters.
     */
    private static boolean isWordMatchsParamsAround(SearchParams params, int paramIndex, ResultSentence result,
            int wordIndex) {
        int startWord = wordIndex - paramIndex;
        if (startWord < 0) {
            return false;
        }
        int endWord = params.words.size() - paramIndex + wordIndex;
        if (endWord > result.words.length) {
            return false;
        }
        for (int i = 0; i < params.words.size(); i++) {
            if (!isWordMatchsParam(params.words.get(i), result.words[startWord + i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is the word corresponds with parameter ?
     */
    private static boolean isWordMatchsParam(SearchParams.Word wordParam, ResultSentence.Word wordResult) {
        if (wordParam.word != null && !wordParam.word.trim().isEmpty()) {
            if (wordParam.allForms) {
                // lemma
                if (!wordParam.lemmas.contains(wordResult.lemma)) {
                    return false;
                }
            } else {
                // concrete form
                if (!wordParam.word.equals(wordResult.value)) {
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
