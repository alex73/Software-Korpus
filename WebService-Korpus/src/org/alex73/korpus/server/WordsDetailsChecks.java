package org.alex73.korpus.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.server.data.ChainRequest;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;

/**
 * Some methods for final checks.
 * 
 * Lucene can't process some complex criteria of search(like order of words,
 * separators, etc.). This class used for such complex checks for filtering of
 * results after Lucene's search.
 * 
 * Methods of instance can be executed in multiple threads.
 */
public class WordsDetailsChecks {
    private final ILanguage lang;
    private final ChainInternals[] chains;
    private final boolean chainsInParagraph;
    private final StaticGrammarFiller2 grFiller;
    private boolean needCheckTags;

    /**
     * Subclass for checking one chain.
     */
    private class ChainInternals {
        private final String separatorBefore;
        private final WordInternals[] words;

        public ChainInternals(ChainRequest chain) {
            this.words = new WordInternals[chain.words.size()];
            this.separatorBefore = chain.seps.get(0);
            for (int i = 0; i < chain.words.size(); i++) {
                this.words[i] = new WordInternals(chain.words.get(i), chain.seps.get(i + 1));
            }
        }
    }

    /**
     * Subclass for checking one word.
     */
    private class WordInternals {
        private final String separatorAfter;
        private String wordNormalized, wordSuperNormalized, wordZnakNormalized;
        private Set<String> formsNormalized, formsSuperNormalized;
        private ThreadLocal<Pattern> wordNormalizedRegexps, wordSuperNormalizedRegexp, wordZnakRegexp, grammarRegexp, grammarVariantRegexp;

        /**
         * Prepare internal representation of checks.
         */
        public WordInternals(WordRequest w, String separatorAfter) {
            ThreadLocal<Pattern> gr = createPatternRegexp(w.grammar);
            switch (w.mode) {
            case USUAL:
                if (w.variants) {
                    String s = nullIfEmpty(lang.getNormalizer().superNormalized(w.word.trim()));
                    if (needWildcardRegexp(s)) {
                        wordSuperNormalizedRegexp = createWildcardRegexp(s);
                    } else {
                        wordSuperNormalized = s;
                    }
                    grammarVariantRegexp = gr;
                } else {
                    String s = nullIfEmpty(lang.getNormalizer().lightNormalized(w.word.trim()));
                    if (needWildcardRegexp(s)) {
                        wordNormalizedRegexps = createWildcardRegexp(s);
                    } else {
                        wordNormalized = s;
                    }
                    grammarRegexp = gr;
                }
                break;
            case ALL_FORMS:
                if (w.variants) {
                    formsSuperNormalized = getForms(w.word, w.variants);
                    grammarVariantRegexp = gr;
                } else {
                    formsNormalized = getForms(w.word, w.variants);
                    grammarRegexp = gr;
                }
                break;
            case EXACT:
                String s = nullIfEmpty(lang.getNormalizer().znakNormalized(w.word.trim()));
                if (needWildcardRegexp(s)) {
                    wordZnakRegexp = createWildcardRegexp(s);
                } else {
                    wordZnakNormalized = s;
                }
                break;
            case GRAMMAR:
                if (w.variants) {
                    grammarVariantRegexp = gr;
                } else {
                    grammarRegexp = gr;
                }
                break;
            default:
                throw new RuntimeException();
            }
            this.separatorAfter = separatorAfter;
            if (grammarRegexp != null || grammarVariantRegexp != null) {
                needCheckTags = true;
            }
        }

        /**
         * Fill forms list for ALL_FORMS mode.
         */
        private Set<String> getForms(String word, boolean variant) {
            Set<String> result = new HashSet<>();
            String expected = variant ? lang.getNormalizer().superNormalized(word) : lang.getNormalizer().lightNormalized(word);
            for (Paradigm p : grFiller.getFinder().getParadigms(word.trim())) {
                boolean collectForms = false;
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        String form = variant ? lang.getNormalizer().superNormalized(f.getValue()) : lang.getNormalizer().lightNormalized(f.getValue());
                        if (expected.equals(form)) {
                            collectForms = true;
                            break;
                        }
                    }
                }
                if (collectForms) {
                    for (Variant v : p.getVariant()) {
                        for (Form f : v.getForm()) {
                            String form = variant ? lang.getNormalizer().superNormalized(f.getValue()) : lang.getNormalizer().lightNormalized(f.getValue());
                            if (!form.isEmpty()) {
                                result.add(form);
                            }
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Check if word is matches.
         */
        private boolean matches(WordResult wordResult) {
            // правяраем слова, і вяртаем 'false' калі не супадае
            if (wordNormalized != null) {
                if (!wordNormalized.equals(wordResult.wordNormalized)) {
                    return false;
                }
            }
            if (wordZnakNormalized != null) {
                if (!wordZnakNormalized.equals(wordResult.wordZnakNormalized)) {
                    return false;
                }
            }
            if (wordSuperNormalized != null) {
                if (!wordSuperNormalized.equals(wordResult.wordSuperNormalized)) {
                    return false;
                }
            }
            if (wordNormalizedRegexps != null) {
                if (!wordNormalizedRegexps.get().matcher(wordResult.wordNormalized).matches()) {
                    return false;
                }
            }
            if (wordZnakRegexp != null) {
                if (!wordZnakRegexp.get().matcher(wordResult.wordZnakNormalized).matches()) {
                    return false;
                }
            }
            if (wordSuperNormalizedRegexp != null) {
                if (!wordSuperNormalizedRegexp.get().matcher(wordResult.wordSuperNormalized).matches()) {
                    return false;
                }
            }
            if (formsNormalized != null) {
                if (!formsNormalized.contains(wordResult.wordNormalized)) {
                    return false;
                }
            }
            if (formsSuperNormalized != null) {
                if (!formsSuperNormalized.contains(wordResult.wordSuperNormalized)) {
                    return false;
                }
            }
            // check grammar
            if (grammarRegexp != null) {
                if (wordResult.tagsNormalized != null) {
                    for (String t : wordResult.tagsNormalized.split(";")) {
                        String dbTag = lang.getDbTags().getDBTagString(t);
                        if (grammarRegexp.get().matcher(dbTag).matches()) {
                            return true;
                        }
                    }
                }
                return false;
            }
            if (grammarVariantRegexp != null) {
                if (wordResult.tagsVariants != null) {
                    for (String t : wordResult.tagsVariants.split(";")) {
                        String dbTag = lang.getDbTags().getDBTagString(t);
                        if (grammarVariantRegexp.get().matcher(dbTag).matches()) {
                            return true;
                        }
                    }
                }
                return false;
            }

            return true;
        }
    }

    /**
     * Only for checking words from grammar database.
     */
    public WordsDetailsChecks() {
        this.lang = null;
        this.chains = null;
        this.chainsInParagraph = false;
        this.grFiller = null;
    }

    /**
     * For checking words from texts.
     */
    public WordsDetailsChecks(ILanguage lang, List<ChainRequest> inputChains, boolean chainsInParagraph, StaticGrammarFiller2 grFiller) {
        this.lang = lang;
        this.chains = new ChainInternals[inputChains.size()];
        this.chainsInParagraph = chainsInParagraph;
        this.grFiller = grFiller;
        for (int i = 0; i < chains.length; i++) {
            chains[i] = new ChainInternals(inputChains.get(i));
        }
    }

    /**
     * Is the document corresponding with search criteria ?
     */
    public boolean isAllowed(Paragraph[] resultText) {
        // fill normalization and tags
        grFiller.fill(resultText, needCheckTags);

        boolean found = false;
        for (Paragraph p : resultText) {
            if (!lang.getLanguage().equals(p.lang)) {
                continue;
            }
            boolean foundInParagraph = false;
            if (chainsInParagraph) {
                if (isChainsInParagraph(p)) {
                    found = true;
                }
            } else {
                for (Sentence s : p.sentences) {
                    if (isChainsInSentence(s)) {
                        found = true;
                    }
                }
            }
            if (foundInParagraph) {
                found = true;
            }
        }
        return found;
    }

    private boolean isChainsInParagraph(Paragraph p) {
        boolean foundInParagraph = true;
        for (ChainInternals chain : chains) {
            boolean chainFound = false;
            for (Sentence s : p.sentences) {
                if (isChainInSentence(chain, s)) {
                    chainFound = true;
                }
            }
            if (!chainFound) {
                foundInParagraph = false;
            }
        }
        return foundInParagraph;
    }

    private boolean isChainsInSentence(Sentence s) {
        boolean chainFound = true;
        for (ChainInternals chain : chains) {
            if (!isChainInSentence(chain, s)) {
                chainFound = false;
            }
        }
        return chainFound;
    }

    public boolean isOneWordAllowed(WordResult wordResult) {
        grFiller.fill(wordResult, needCheckTags);

        if (chains.length != 1 || chains[0].words.length != 1) {
            throw new RuntimeException();
        }
        return chains[0].words[0].matches(wordResult);
    }

    private boolean isChainInSentence(ChainInternals chain, Sentence s) {
        boolean found = false;
        for (int j = 0; j < s.words.length; j++) {
            WordResult[] result = isWordsMatchChain(lang, chain, s, j);
            if (result != null) {
                for (WordResult rs : result) {
                    // Check and mark requested words for highlight for user.
                    rs.requestedWord = true;
                }
                found = true; // don't stop comparison(i.e.break), because we need to mark all occurrences
            }
        }
        return found;
    }

    /**
     * Checks if sentence correspond with chain starting from specific position.
     * 
     * Returns list of words in case of matches, or null otherwise.
     */
    private WordResult[] isWordsMatchChain(ILanguage lang, ChainInternals chain, Sentence sentence, int position) {
        WordResult[] checks = new WordResult[chain.words.length];
        int count = 0;
        for (int i = position; i < sentence.words.length && count < checks.length; i++) {
            if (sentence.words[i].type != Word.OtherType.PAZNAKA) { // прапускаем пазнакі
                checks[count] = (WordResult) sentence.words[i];
                count++;
            }
        }
        if (count < checks.length) {
            return null; // недастаткова слоў
        }
        for (int i = 0; i < checks.length; i++) {
            if (!chain.words[i].matches(checks[i])) {
                return null;
            }
        }
        if (chain.separatorBefore != null) {
            // шукаем перад словамі
            Word before = null;
            for (int i = position - 1; i >= 0; i--) {
                if (sentence.words[i].type != Word.OtherType.PAZNAKA) { // прапускаем пазнакі
                    before = sentence.words[i];
                    break;
                }
            }
            if ("^".equals(chain.separatorBefore)) {
                if (before != null) {
                    return null;
                }
            } else {
                if (before == null) {
                    return null;
                }
                // TODO check value
            }
        }
        for (int i = 0; i < checks.length; i++) {
            if (!isOneSepMatches(chain.words[i].separatorAfter, checks[i])) {
                return null;
            }
        }
        return checks;
    }

    private boolean isOneSepMatches(String requiredSeparator, WordResult wordResult) {
        return true; // TODO
    }

    public static boolean isTooSimpleWord(WordRequest w) {
        if (w.word == null) {
            return w.grammar == null;
        }
        if (w.word.replace("+", "").replace("*", "").replace("?", "").length() >= 2) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean needWildcardRegexp(String word) {
        if (word == null) {
            return false;
        }
        return word.contains("*") || word.contains("?");
    }

    private static String nullIfEmpty(String s) {
        return s.isEmpty() ? null : s;
    }

    /**
     * Usually for words, like "нач*"
     */
    public ThreadLocal<Pattern> createWildcardRegexp(String wildcardWord) {
        if (wildcardWord == null) {
            return null;
        }
        // need ThreadLocal because Pattern doesn't support multithreads check
        return new ThreadLocal<Pattern>() {
            @Override
            protected Pattern initialValue() {
                return Pattern.compile(wildcardWord.replace("*", ".*").replace('?', '.'));
            }
        };
    }

    /**
     * Usually for grammar, like "N...[23]..."
     */
    public ThreadLocal<Pattern> createPatternRegexp(String regexp) {
        if (regexp == null) {
            return null;
        }
        // need ThreadLocal because Pattern doesn't support multithreads check
        return new ThreadLocal<Pattern>() {
            @Override
            protected Pattern initialValue() {
                return Pattern.compile(regexp);
            }
        };
    }
}
