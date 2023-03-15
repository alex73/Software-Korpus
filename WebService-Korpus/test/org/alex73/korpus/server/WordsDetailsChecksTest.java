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
    public void isOneWordMatches() throws Exception {
        assertTrue(check(WordMode.USUAL, false, null, "снег", "снег"));
        assertTrue(check(WordMode.USUAL, false, null, "Сне\u00B4г", "снег"));
        assertTrue(check(WordMode.USUAL, false, null, "снег", "Сне\u00B4г"));
        assertFalse(check(WordMode.USUAL, false, null, "снег", "сьнег"));
        assertFalse(check(WordMode.USUAL, false, null, "снег", "снега"));
        assertTrue(check(WordMode.USUAL, false, null, "снег?", "Снега"));
        assertTrue(check(WordMode.USUAL, false, null, "снег*", "Снег"));
        assertTrue(check(WordMode.USUAL, false, null, "снег*", "Снега"));
        assertTrue(check(WordMode.USUAL, false, null, "увага", "ўвага"));
        assertTrue(check(WordMode.USUAL, false, null, "ўвага", "увага"));

        assertTrue(check(WordMode.EXACT, false, null, "сне\u00B4г", "снег"));
        assertTrue(check(WordMode.EXACT, false, null, "снег", "сне\u00B4г"));
        assertFalse(check(WordMode.EXACT, false, null, "снег*", "Снега"));
        assertFalse(check(WordMode.EXACT, false, null, "снег*", "Снега"));
        assertTrue(check(WordMode.EXACT, false, null, "ўвага", "ўвага"));
        assertFalse(check(WordMode.EXACT, false, null, "увага", "ўвага"));
        assertFalse(check(WordMode.EXACT, false, null, "ўвага", "увага"));
        assertFalse(check(WordMode.EXACT, true, null, "снег", "сьнег"));
        assertFalse(check(WordMode.EXACT, true, null, "снег", "Снег"));

        assertTrue(check(WordMode.USUAL, true, null, "сьнег", "снег"));
        assertTrue(check(WordMode.USUAL, true, null, "снег", "сьнег"));
        assertTrue(check(WordMode.USUAL, true, null, "сьнег", "Снэ\u00B4г"));
        assertFalse(check(WordMode.USUAL, true, null, "сьнег", "Сняг"));

        assertTrue(check(WordMode.ALL_FORMS, false, null, "снег", "снягі"));
        assertFalse(check(WordMode.ALL_FORMS, false, null, "снег", "снягіі"));

        assertFalse(check(WordMode.ALL_FORMS, false, null, "снег", "сьнягі"));
        assertTrue(check(WordMode.ALL_FORMS, true, null, "снег", "сьнягі"));

        assertFalse(check(WordMode.GRAMMAR, false, "N.........NP", null, "сьнягі"));
        assertTrue(check(WordMode.GRAMMAR, true, "N.........NP", null, "сьнягі"));
        assertTrue(check(WordMode.GRAMMAR, true, "N.........LP", null, "снегу"));
        assertFalse(check(WordMode.GRAMMAR, false, "N.........LP", null, "снегу"));
    }

    private boolean check(WordRequest.WordMode mode, boolean variants, String grammar, String rqWord, String word) {
        ILanguage bel = LanguageFactory.get("bel");
        WordRequest rq = new WordRequest();
        rq.mode = mode;
        rq.variants = variants;
        rq.word = rqWord;
        rq.grammar = grammar;
        ChainRequest chain = new ChainRequest();
        chain.words = List.of(rq);
        chain.seps = Arrays.asList(null, null);
        WordsDetailsChecks check = new WordsDetailsChecks(bel, List.of(chain), false, grFiller);
        WordResult rs = new WordResult(new Word());
        rs.word = word;
        rs.wordNormalized = bel.getNormalizer().lightNormalized(rs.word);
        rs.wordZnakNormalized = bel.getNormalizer().znakNormalized(rs.word);
        rs.wordSuperNormalized = bel.getNormalizer().superNormalized(rs.word);
        return check.isOneWordAllowed(rs);
    }

    @Test
    public void checkMultipleWords() {
        assertTrue(check(List.of(List.of("снег"), List.of("навокал")), true, List.of(List.of("снег"), List.of("навокал"))));
        assertTrue(check(List.of(List.of("снег"), List.of("навокал")), false, List.of(List.of("снег", "навокал"))));
        assertFalse(check(List.of(List.of("снег"), List.of("навокал")), false, List.of(List.of("снег"), List.of("навокал"))));
    }

    private boolean check(List<List<String>> requires, boolean chainsInParagraph, List<List<String>> text) {
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
        return check.isAllowed(new Paragraph[] { p });
    }

    @Test
    public void checkWordsAround() {
        assertTrue(check(List.of("снег", "навокал"), Arrays.asList("снег", "навокал")));
        assertFalse(check(List.of("снег", "навокал"), Arrays.asList("снег", "ляжыць", "навокал")));
        assertTrue(check(List.of("снег", "навокал"), Arrays.asList("снег", null, "навокал")));
        assertFalse(check(List.of("снег", "навокал"), List.of("навокал", "снег")));
    }

    private boolean check(List<String> ch, List<String> text) {
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
        return check.isAllowed(new Paragraph[] { p });
    }
}
