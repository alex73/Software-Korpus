package org.alex73.korpus.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.ChainRequest;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordRequest.WordMode;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.junit.Test;

public class WordsDetailsChecksTest {
    private GrammarDB2 gr;
    private GrammarFinder grFinder;
    private StaticGrammarFiller2 grFiller;

    public WordsDetailsChecksTest() throws Exception {
        gr = GrammarDB2.initializeFromFile(new File("test/org/alex73/korpus/server/WordsDetailsChecksTest.xml"));
        grFinder = new GrammarFinder(gr);
        grFiller = new StaticGrammarFiller2(grFinder);
    }

    @Test
    public void testOneWordMatches() throws Exception {
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег", "снег"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "Сне\u00B4г", "снег"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег", "Сне\u00B4г"));
        assertFalse(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег", "сьнег"));
        assertFalse(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег", "снега"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег?", "Снега"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег*", "Снег"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "снег*", "Снега"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "увага", "ўвага"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, false, false, null, "ўвага", "увага"));

        assertTrue(checkOneWordMatches(WordMode.EXACT, false, false, null, "сне\u00B4г", "снег"));
        assertTrue(checkOneWordMatches(WordMode.EXACT, false, false, null, "снег", "сне\u00B4г"));
        assertFalse(checkOneWordMatches(WordMode.EXACT, false, false, null, "снег*", "Снега"));
        assertFalse(checkOneWordMatches(WordMode.EXACT, false, false, null, "снег*", "Снега"));
        assertTrue(checkOneWordMatches(WordMode.EXACT, false, false, null, "ўвага", "ўвага"));
        assertFalse(checkOneWordMatches(WordMode.EXACT, false, false, null, "увага", "ўвага"));
        assertFalse(checkOneWordMatches(WordMode.EXACT, false, false, null, "ўвага", "увага"));
        assertFalse(checkOneWordMatches(WordMode.EXACT, true, false, null, "снег", "сьнег"));
        assertFalse(checkOneWordMatches(WordMode.EXACT, true, false, null, "снег", "Снег"));
        assertTrue(checkOneWordMatches(WordMode.EXACT, true, true, null, "сь?нег", "сьнег"));
        assertTrue(checkOneWordMatches(WordMode.EXACT, true, true, null, "сне[гх]", "снег"));

        assertTrue(checkOneWordMatches(WordMode.USUAL, true, false, null, "сьнег", "снег"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, true, false, null, "снег", "сьнег"));
        assertTrue(checkOneWordMatches(WordMode.USUAL, true, false, null, "сьнег", "Снэ\u00B4г"));
        assertFalse(checkOneWordMatches(WordMode.USUAL, true, false, null, "сьнег", "Сняг"));

        assertTrue(checkOneWordMatches(WordMode.ALL_FORMS, false, false, null, "снег", "снягі"));
        assertFalse(checkOneWordMatches(WordMode.ALL_FORMS, false, false, null, "снег", "снягіі"));

        assertFalse(checkOneWordMatches(WordMode.ALL_FORMS, false, false, null, "снег", "сьнягі"));
        assertTrue(checkOneWordMatches(WordMode.ALL_FORMS, true, false, null, "снег", "сьнягі"));

        assertFalse(checkOneWordMatches(WordMode.GRAMMAR, false, false, "N.........NP", null, "сьнягі"));
        assertTrue(checkOneWordMatches(WordMode.GRAMMAR, true, false, "N.........NP", null, "сьнягі"));
        assertTrue(checkOneWordMatches(WordMode.GRAMMAR, true, false, "N.........LP", null, "снегу"));
        assertFalse(checkOneWordMatches(WordMode.GRAMMAR, false, false, "N.........LP", null, "снегу"));
    }

    private boolean checkOneWordMatches(WordRequest.WordMode mode, boolean variants, boolean regexp, String grammar, String rqWord, String word) {
        ILanguage bel = LanguageFactory.get("bel");
        WordRequest rq = new WordRequest();
        rq.mode = mode;
        rq.variants = variants;
        rq.word = rqWord;
        rq.regexp = regexp;
        rq.grammar = grammar;
        ChainRequest chain = new ChainRequest();
        chain.words = List.of(rq);
        chain.seps = Arrays.asList(null, null);
        WordsDetailsChecks check = new WordsDetailsChecks(bel, List.of(chain), false, grFiller);
        WordResult rs = new WordResult(new Word());
        rs.word = word;
        return check.isOneWordAllowed(new TextInfo(), rs);
    }

    @Test
    public void testMultipleWords() {
        assertTrue(checkMultipleWords(List.of(List.of("снег"), List.of("навокал")), true, List.of(List.of("снег"), List.of("навокал"))));
        assertTrue(checkMultipleWords(List.of(List.of("снег"), List.of("навокал")), false, List.of(List.of("снег", "навокал"))));
        assertFalse(checkMultipleWords(List.of(List.of("снег"), List.of("навокал")), false, List.of(List.of("снег"), List.of("навокал"))));
    }

    private boolean checkMultipleWords(List<List<String>> requires, boolean chainsInParagraph, List<List<String>> text) {
        ILanguage bel = LanguageFactory.get("bel");
        List<ChainRequest> chains = new ArrayList<>();
        for (List<String> ch : requires) {
            ChainRequest chain = new ChainRequest();
            chain.words = ch.stream().map(w -> {
                WordRequest rq = new WordRequest();
                rq.mode = WordMode.USUAL;
                rq.variants = false;
                rq.word = w;
                return rq;
            }).collect(Collectors.toList());
            chain.seps = new ArrayList<>();
            chain.seps.add(null);
            ch.forEach(w -> chain.seps.add(null));
            chains.add(chain);
        }

        WordsDetailsChecks check = new WordsDetailsChecks(bel, chains, chainsInParagraph, grFiller);

        Paragraph p = new Paragraph();
        p.lang = "bel";
        p.sentences = new Sentence[text.size()];
        for (int i = 0; i < p.sentences.length; i++) {
            p.sentences[i] = new Sentence();
            p.sentences[i].words = new WordResult[text.get(i).size()];
            for (int j = 0; j < text.get(i).size(); j++) {
                p.sentences[i].words[j] = new WordResult(new Word());
                p.sentences[i].words[j].word = text.get(i).get(j);
            }
        }
        return check.isAllowed(new TextInfo(), new Paragraph[] { p });
    }

    @Test
    public void testWordsAround() {
        assertTrue(checkWordsAround(List.of("снег", "навокал"), Arrays.asList("снег", "навокал")));
        assertFalse(checkWordsAround(List.of("снег", "навокал"), Arrays.asList("снег", "ляжыць", "навокал")));
        assertTrue(checkWordsAround(List.of("снег", "навокал"), Arrays.asList("снег", null, "навокал")));
        assertFalse(checkWordsAround(List.of("снег", "навокал"), List.of("навокал", "снег")));
    }

    private boolean checkWordsAround(List<String> ch, List<String> text) {
        ILanguage bel = LanguageFactory.get("bel");
        ChainRequest chain = new ChainRequest();
        chain.words = ch.stream().map(w -> {
            WordRequest rq = new WordRequest();
            rq.mode = WordMode.USUAL;
            rq.variants = false;
            rq.word = w;
            return rq;
        }).collect(Collectors.toList());
        chain.seps = new ArrayList<>();
        chain.seps.add(null);
        ch.forEach(w -> chain.seps.add(null));

        WordsDetailsChecks check = new WordsDetailsChecks(bel, List.of(chain), false, grFiller);

        Paragraph p = new Paragraph();
        p.lang = "bel";
        p.sentences = new Sentence[1];
        p.sentences[0] = new Sentence();
        p.sentences[0].words = new WordResult[text.size()];
        for (int j = 0; j < text.size(); j++) {
            p.sentences[0].words[j] = new WordResult(new Word());
            if (text.get(j) == null) {
                p.sentences[0].words[j].type = Word.OtherType.PAZNAKA;
            } else {
                p.sentences[0].words[j].word = text.get(j);
            }
        }
        return check.isAllowed(new TextInfo(), new Paragraph[] { p });
    }

    @Test
    public void testSeparators() {
        assertTrue(checkSeparators(Arrays.asList(null, "а", "$"), Arrays.asList("б", " ", "а", ".")));
        assertTrue(checkSeparators(Arrays.asList(null, "а", "."), Arrays.asList("б", " ", "а", ".")));
        assertFalse(checkSeparators(Arrays.asList(null, "а", ","), Arrays.asList("б", " ", "а", ".")));
        assertFalse(checkSeparators(Arrays.asList(",", "а", null), Arrays.asList("б", " ", "а", ".")));
        assertFalse(checkSeparators(Arrays.asList(null, "б", "$"), Arrays.asList("б", " ", "а", ".")));
        assertTrue(checkSeparators(Arrays.asList("^", "а", null), Arrays.asList("а", " ", "б", ".")));
        assertTrue(checkSeparators(Arrays.asList("^", "а", ""), Arrays.asList("а", " ", "б", ".")));
        assertFalse(checkSeparators(Arrays.asList("^", "а", " "), Arrays.asList("а", " ", "б", ".")));
        assertFalse(checkSeparators(Arrays.asList("^", "а", ""), Arrays.asList(null, "-", "а", " ", "б", ".")));
        assertTrue(checkSeparators(Arrays.asList("-", "а", ""), Arrays.asList(null, "-", "а", " ", "б", ".")));
        assertTrue(checkSeparators(Arrays.asList(",", "а", null), Arrays.asList(",б", ", ", "а", ".")));
        assertFalse(checkSeparators(Arrays.asList(",", "а", null), Arrays.asList(",б", ": ", "а", ".")));
    }

    private boolean checkSeparators(List<String> expect, List<String> text) {
        ILanguage bel = LanguageFactory.get("bel");
        ChainRequest chain = new ChainRequest();
        chain.words = new ArrayList<>();
        chain.seps = new ArrayList<>();
        chain.seps.add(expect.get(0));
        for (int i = 1; i < expect.size(); i += 2) {
            WordRequest rq = new WordRequest();
            rq.mode = WordMode.USUAL;
            rq.variants = false;
            rq.word = expect.get(i);
            chain.words.add(rq);
            chain.seps.add(expect.get(i + 1));
        }

        WordsDetailsChecks check = new WordsDetailsChecks(bel, List.of(chain), false, grFiller);

        Paragraph p = new Paragraph();
        p.lang = "bel";
        p.sentences = new Sentence[1];
        p.sentences[0] = new Sentence();
        p.sentences[0].words = new WordResult[text.size() / 2];
        for (int i = 0; i < text.size(); i += 2) {
            p.sentences[0].words[i / 2] = new WordResult(new Word());
            p.sentences[0].words[i / 2].word = text.get(i);
            p.sentences[0].words[i / 2].tail = text.get(i + 1);
        }
        return check.isAllowed(new TextInfo(), new Paragraph[] { p });
    }
}
