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

import org.alex73.korpus.base.DBTagsGroups;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.server.data.SearchParams;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

/**
 * Some methods for final checks.
 * 
 * Lucene can't process some complex criteria of search(like order of words).
 * This methods used for these complex check for filtering of results from
 * Lucene's search.
 */
public class WordsDetailsChecks {

    /**
     * Is the document correspond with search criteria ? Check and mark requested
     * words for highlight for user.
     */
    public static boolean isAllowed(SearchParams.WordsOrder wordsOrder, List<WordRequest> words, Paragraph resultText) {
        boolean found = false;
        switch (wordsOrder) {
        case PRESET:
            for (int i = 0; i < resultText.sentences.length; i++) {
                for (int j = 0; j < resultText.sentences[i].words.length; j++) {
                    if (isWordsAroundMatchParams(words, resultText.sentences[i], j)) {
                        for (int k = 0, count = 0; count < words.size(); k++) {
                            // mark found words as requested
                            ((WordResult) resultText.sentences[i].words[j + k]).requestedWord = true;
                            count++;
                        }
                        found = true;
                    }
                }
            }
            break;
        case ANY_IN_SENTENCE:
            for (int i = 0; i < resultText.sentences.length; i++) {
                int c = 0;
                for (WordRequest pw : words) {
                    boolean foundWord = false;
                    for (int j = 0; j < resultText.sentences[i].words.length; j++) {
                        if (isOneWordMatchsParam(pw, (WordResult) resultText.sentences[i].words[j])) {
                            ((WordResult) resultText.sentences[i].words[j]).requestedWord = true;
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
                for (int i = 0; i < resultText.sentences.length; i++) {
                    for (int j = 0; j < resultText.sentences[i].words.length; j++) {
                        if (isOneWordMatchsParam(pw, (WordResult) resultText.sentences[i].words[j])) {
                            ((WordResult) resultText.sentences[i].words[j]).requestedWord = true;
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
     * For the wordsOrder=PRESET: specified words should correspond with specified
     * parameter and all other parameters.
     */
    private static boolean isWordsAroundMatchParams(List<WordRequest> words, Sentence resultWords, int wordIndex) {
        WordResult[] checks = new WordResult[words.size()];
        int count = 0;
        for (int i = wordIndex; i < resultWords.words.length && count < words.size(); i++) {
            checks[count] = (WordResult) resultWords.words[i];
            count++;
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
        if (wordResult.normalized == null) {
            return false;
        }
        if (wordParam.word != null && !wordParam.word.trim().isEmpty()) {
            if (wordParam.allForms) {
                // lemma
                if (wordResult.lemmas == null) {
                    return false;
                }
                boolean found = false;
                for (String expectedLemma : wordParam.lemmas) {
                    if (SetUtils.inSeparatedList(wordResult.lemmas, expectedLemma)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            } else {
                // concrete form
                if (needWildcardRegexp(wordParam.word)) {
                    if (wordResult.normalized == null) {
                        return false;
                    }
                    if (!getWildcardRegexp(wordParam.word).matcher(StressUtils.unstress(wordResult.normalized)).matches()) {
                        return false;
                    }
                } else if (!BelarusianWordNormalizer.equals(wordParam.word, wordResult.normalized)) {
                    return false;
                }
            }
        }
        if (wordParam.grammar == null) {
            return true;
        }
        if (wordResult.tags == null) {
            return false;
        }
        // check grammar
        Pattern reGrammar = getPatternRegexp(wordParam.grammar);
        for (String t : wordResult.tags.split(";")) {
            String dbTag = DBTagsGroups.getDBTagString(t);
            if (reGrammar.matcher(dbTag).matches()) {
                return true;
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
        if (needWildcardRegexp(w.word)) {
            // contains wildcards
            if (w.allForms) {
                return true;
            } else if (wt.replace("+", "").replace("*", "").replace("?", "").length() >= 2) {
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
        PATTERN_REGEXPS.get().clear();
    }

    /**
     * Usually for words, like "нач*"
     */
    private static ThreadLocal<Map<String, Pattern>> WILDCARD_REGEXPS = new ThreadLocal<Map<String, Pattern>>() {
        @Override
        protected Map<String, Pattern> initialValue() {
            return new TreeMap<>();
        }
    };

    public static boolean needWildcardRegexp(String word) {
        return word.contains("*") || word.contains("?");
    }

    public static Pattern getWildcardRegexp(String wildcardWord) {
        Pattern p = WILDCARD_REGEXPS.get().get(wildcardWord);
        if (p == null) {
            p = Pattern.compile(wildcardWord.replace("+", "").replace("*", ".*").replace('?', '.'));
            WILDCARD_REGEXPS.get().put(wildcardWord, p);
        }
        return p;
    }

    /**
     * Usually for grammar, like "N...[23]..."
     */
    private static ThreadLocal<Map<String, Pattern>> PATTERN_REGEXPS = new ThreadLocal<Map<String, Pattern>>() {
        @Override
        protected Map<String, Pattern> initialValue() {
            return new TreeMap<>();
        }
    };

    public static Pattern getPatternRegexp(String regexp) {
        Pattern p = PATTERN_REGEXPS.get().get(regexp);
        if (p == null) {
            p = Pattern.compile(regexp);
            PATTERN_REGEXPS.get().put(regexp, p);
        }
        return p;
    }
}
