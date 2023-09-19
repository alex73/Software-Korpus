package org.alex73.korpus.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.server.data.ChainRequest;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordRequest.WordMode;
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
    private ILanguage lang;
    private ChainInternals[] chains;
    private boolean chainsInParagraph;
    private StaticGrammarFiller2 grFiller;
    private boolean needCheckTags;

    /**
     * Subclass for checking one chain.
     */
    private class ChainInternals {
        private final String separatorBefore;
        private final WordInternals[] words;

        public ChainInternals(ChainRequest chain) {
            this.words = new WordInternals[chain.words.size()];
            if (chain.seps != null) {
                String s = chain.seps.get(0);
                this.separatorBefore = s == null || s.isEmpty() ? null : s;
            } else {
                this.separatorBefore = null;
            }
            for (int i = 0; i < chain.words.size(); i++) {
                this.words[i] = new WordInternals(chain.words.get(i), chain.seps == null ? null : chain.seps.get(i + 1));
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
                    String s = nullIfEmpty(lang.getNormalizer().superNormalized(w.word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS));
                    if (needMasks(w) != NEED_MASKS.NO) {
                        wordSuperNormalizedRegexp = createWildcardRegexp(s);
                    } else {
                        wordSuperNormalized = s;
                    }
                    grammarVariantRegexp = gr;
                } else {
                    String s = nullIfEmpty(lang.getNormalizer().lightNormalized(w.word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS));
                    if (needMasks(w) != NEED_MASKS.NO) {
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
                switch (needMasks(w)) {
                case NO:
                    wordZnakNormalized = nullIfEmpty(lang.getNormalizer().znakNormalized(w.word.trim(), ILanguage.INormalizer.PRESERVE_NONE));
                    break;
                case WILDCARDS:
                    wordZnakRegexp = createWildcardRegexp(
                            nullIfEmpty(lang.getNormalizer().znakNormalized(w.word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS)));
                    break;
                case REGEX:
                    wordZnakRegexp = createPatternRegexp(
                            nullIfEmpty(lang.getNormalizer().znakNormalized(w.word.trim(), ILanguage.INormalizer.PRESERVE_REGEXP)));
                    break;
                default:
                    throw new RuntimeException();
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
            String expected = variant ? lang.getNormalizer().superNormalized(word, ILanguage.INormalizer.PRESERVE_NONE)
                    : lang.getNormalizer().lightNormalized(word, ILanguage.INormalizer.PRESERVE_NONE);
            for (Paradigm p : grFiller.getFinder().getParadigms(word.trim())) {
                boolean collectForms = false;
                for (Variant v : p.getVariant()) {
                    for (Form f : v.getForm()) {
                        String form = variant ? lang.getNormalizer().superNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE)
                                : lang.getNormalizer().lightNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE);
                        if (expected.equals(form)) {
                            collectForms = true;
                            break;
                        }
                    }
                }
                if (collectForms) {
                    for (Variant v : p.getVariant()) {
                        for (Form f : v.getForm()) {
                            String form = variant ? lang.getNormalizer().superNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE)
                                    : lang.getNormalizer().lightNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE);
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
    private WordsDetailsChecks() {
    }

    public static WordsDetailsChecks createForGrammarDB() {
        return new WordsDetailsChecks();
    }

    /**
     * For checking words from texts.
     */
    public static WordsDetailsChecks createForSearch(ILanguage lang, List<ChainRequest> inputChains, boolean chainsInParagraph, StaticGrammarFiller2 grFiller) {
        WordsDetailsChecks r = new WordsDetailsChecks();
        r.lang = lang;
        r.chains = new ChainInternals[inputChains.size()];
        r.chainsInParagraph = chainsInParagraph;
        r.grFiller = grFiller;
        for (int i = 0; i < r.chains.length; i++) {
            r.chains[i] = r.new ChainInternals(inputChains.get(i));
        }
        return r;
    }

    public static WordsDetailsChecks createForCluster(ILanguage lang, ChainRequest inputChain, StaticGrammarFiller2 grFiller) {
        WordsDetailsChecks r = new WordsDetailsChecks();
        r.lang = lang;
        r.grFiller = grFiller;
        r.chains = new ChainInternals[1];
        inputChain.seps = null;
        r.chains[0] = r.new ChainInternals(inputChain);
        return r;
    }

    /**
     * Is the document corresponding with search criteria ?
     */
    public boolean isAllowed(TextInfo textInfo, Paragraph[] resultText) {
        for (TextInfo.Subtext tis : textInfo.subtexts) {
            if (ApplicationKorpus.instance.isAuthorsBlacklisted(tis.authors)) {
                return false;
            }
        }
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

    public boolean isOneWordAllowed(TextInfo textInfo, WordResult wordResult) {
        for (TextInfo.Subtext tis : textInfo.subtexts) {
            if (ApplicationKorpus.instance.isAuthorsBlacklisted(tis.authors)) {
                return false;
            }
        }
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
        if (sentence.words[position].type == Word.OtherType.PAZNAKA || sentence.words[position].wordNormalized == null) {
            // should be started from exact this word
            return null;
        }
        WordResult[] checks = new WordResult[chain.words.length];
        int count = 0;
        int positionAfter = 0;
        for (int i = position; i < sentence.words.length && count < checks.length; i++) {
            if (sentence.words[i].type != Word.OtherType.PAZNAKA && sentence.words[i].wordNormalized != null) {
                // прапускаем пазнакі і пустыя знакі перад пачаткам сказу
                checks[count] = (WordResult) sentence.words[i];
                count++;
            }
            positionAfter = i + 1;
        }
        if (count < checks.length) {
            return null; // недастаткова слоў
        }
        for (int i = 0; i < checks.length; i++) {
            if (!chain.words[i].matches(checks[i])) {
                return null;
            }
        }
        // TODO Калі пазначылі нейкія знакі прыпынку, хоць адзін з іх мусіць быць, і не
        // мусіць быць ніякіх акрамя пазначаных.
        if (chain.separatorBefore != null) {
            // шукаем перад словамі
            Word before = getWordBefore(sentence, position - 1);
            if ("^".equals(chain.separatorBefore)) {
                if (before != null) {
                    return null;
                }
            } else {
                if (before == null) {
                    return null;
                }
                if (!isOneSepMatches(chain.separatorBefore, before)) {
                    return null;
                }
            }
        }
        for (int i = 0; i < checks.length; i++) {
            if (i == checks.length - 1 && "$".equals(chain.words[i].separatorAfter)) {
                // шукаем канец сказу пасля апошняга слову
                Word after = getWordAfter(sentence, positionAfter);
                if (after != null) {
                    return null;
                }
            } else if (!isOneSepMatches(chain.words[i].separatorAfter, checks[i])) {
                return null;
            }
        }
        return checks;
    }

    private Word getWordBefore(Sentence sentence, int position) {
        for (int i = position; i >= 0; i--) {
            if (sentence.words[i].type != Word.OtherType.PAZNAKA) { // прапускаем пазнакі
                return sentence.words[i];
            }
        }
        return null;
    }

    private Word getWordAfter(Sentence sentence, int position) {
        for (int i = position; i < sentence.words.length; i++) {
            if (sentence.words[i].type != Word.OtherType.PAZNAKA) { // прапускаем пазнакі
                return sentence.words[i];
            }
        }
        return null;
    }

    private boolean isOneSepMatches(String requiredSeparator, Word wordResult) {
        if (requiredSeparator == null || requiredSeparator.isEmpty()) {
            return true;
        }
        if ("NONE".equals(requiredSeparator) && wordResult.tail.isBlank()) {
            return true;
        }
        return requiredSeparator.equals(wordResult.tail.replace(" ", ""));
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

    public enum NEED_MASKS {
        NO, WILDCARDS, REGEX
    };

    public static NEED_MASKS needMasks(WordRequest w) {
        if (w.word == null) {
            return NEED_MASKS.NO;
        }
        if (w.regexp && w.mode == WordMode.EXACT) {
            return NEED_MASKS.REGEX;
        }
        if (w.word.contains("*") || w.word.contains("?")) {
            return NEED_MASKS.WILDCARDS;
        }
        return NEED_MASKS.NO;
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

//TODO квадратныя дужки ў тэксце адкідаць
