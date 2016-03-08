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
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

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
                    if (resultText.words[i][j].isWord
                            && isWordsAroundMatchParams(words, resultText.words[i], j)) {
                        for (int k = 0, count = 0; count < words.size(); k++) {
                            // mark found words as requested
                            if (resultText.words[i][j + k].isWord) {
                                resultText.words[i][j + k].requestedWord = true;
                                count++;
                            }
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
                        if (resultText.words[i][j].isWord && isOneWordMatchsParam(pw, resultText.words[i][j])) {
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
                        if (resultText.words[i][j].isWord && isOneWordMatchsParam(pw, resultText.words[i][j])) {
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
    private static boolean isWordsAroundMatchParams(List<WordRequest> words, WordResult[] resultWords,
            int wordIndex) {
        WordResult[] checks = new WordResult[words.size()];
        int count = 0;
        for (int i = wordIndex; i < resultWords.length && count < words.size(); i++) {
            if (resultWords[i].isWord) {
                checks[count] = resultWords[i];
                count++;
            }
        }
        if (count < words.size()) {
            return false;
        }
        for (int i = 0; i < words.size(); i++) {
            if (!isOneWordMatchsParam(words.get(i), checks[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is the word corresponds with parameter ?
     */
    public static boolean isOneWordMatchsParam(WordRequest wordParam, WordResult wordResult) {
        if (wordParam.word != null && !wordParam.word.trim().isEmpty()) {
            if (wordParam.allForms) {
                // lemma
                if (!wordParam.lemmas.contains(wordResult.lemma)) {
                    return false;
                }
            } else {
                // concrete form
                if (wordParam.isWildcardWord()) {
                    if (!getWildcardRegexp(wordParam.word).matcher(wordResult.value).matches()) {
                        return false;
                    }
                } else {
                    if (!wordParam.word.equalsIgnoreCase(wordResult.value)) {
                        return false;
                    }
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
        Pattern reGrammar = getRegexp(wordParam.grammar);
        for (String t : wordResult.cat.split("_")) {
            if (BelarusianTags.getInstance().isValid(t, null)) {
                try {
                    String findTag = DBTagsGroups.getDBTagString(t);
                    if (reGrammar.matcher(findTag).matches()) {
                        return true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return false;
    }

    public static boolean isTooSimpleWord(WordRequest w) {
        if (w.word == null) {
            return true;
        }
        String wt = w.word.trim();
        if (wt.isEmpty()) {
            if (w.grammar != null) {
                String gt = w.grammar.trim();
                if (!gt.startsWith("K")) {
                    return false;
                }
            }
        }
        if (w.isWildcardWord()) {
            // contains wildcards
            if (w.allForms) {
                return true;
            } else if (wt.replace("*", "").replace("?", "").length() > 1) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static void reset() {
        WILDCARD_REGEXPS.get().clear();
        REGEXPS.get().clear();
    }

    private static ThreadLocal<Map<String, Pattern>> WILDCARD_REGEXPS = new ThreadLocal<Map<String, Pattern>>() {
        @Override
        protected Map<String, Pattern> initialValue() {
            return new TreeMap<>();
        }
    };

    private static Pattern getWildcardRegexp(String wildcardWord) {
        Pattern p = WILDCARD_REGEXPS.get().get(wildcardWord);
        if (p == null) {
            p = Pattern.compile(wildcardWord.replace("*", ".*").replace('?', '.'));
            WILDCARD_REGEXPS.get().put(wildcardWord, p);
        }
        return p;
    }

    private static ThreadLocal<Map<String, Pattern>> REGEXPS = new ThreadLocal<Map<String, Pattern>>() {
        @Override
        protected Map<String, Pattern> initialValue() {
            return new TreeMap<>();
        }
    };

    private static Pattern getRegexp(String regexp) {
        Pattern p = REGEXPS.get().get(regexp);
        if (p == null) {
            p = Pattern.compile(regexp);
            REGEXPS.get().put(regexp, p);
        }
        return p;
    }
}
