package org.alex73.korpus.server;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.korpus.base.StaticGrammarFiller2;
import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.server.data.ChainRequest;
import org.alex73.korpus.server.data.WordRequest;
import org.alex73.korpus.server.data.WordRequest.WordMode;
import org.alex73.korpus.server.data.WordResult;
import org.alex73.korpus.text.structure.corpus.Paragraph;
import org.alex73.korpus.text.structure.corpus.Sentence;
import org.alex73.korpus.text.structure.corpus.Word;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WordsDetailsChecks unit tests")
class WordsDetailsChecksTest {
    private final StaticGrammarFiller2 grFiller;

    WordsDetailsChecksTest() throws Exception {
        // Search for the XML file in common locations
        var possiblePaths = List.of(
            Path.of("src/test/java/org/alex73/korpus/server/WordsDetailsChecksTest.xml"),
            Path.of("WebService-Korpus/src/test/java/org/alex73/korpus/server/WordsDetailsChecksTest.xml"),
            Path.of("test/org/alex73/korpus/server/WordsDetailsChecksTest.xml")
        );

        File xmlFile = possiblePaths.stream()
            .map(Path::toFile)
            .filter(File::exists)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Could not find WordsDetailsChecksTest.xml"));

        var gr = GrammarDB2.initializeFromFile(xmlFile);
        var grFinder = new GrammarFinder(gr);
        grFiller = new StaticGrammarFiller2(grFinder);
    }

    @ParameterizedTest(name = "Mode {0}: {4} matches {5} (variants={1}, regexp={2}, grammar={3})")
    @CsvSource(delimiter = '|', value = {
        "USUAL     | false | false |              | снег       | снег   ",
        "USUAL     | false | false |              | Сне\u00B4г    | снег   ",
        "USUAL     | false | false |              | снег       | Сне\u00B4г ",
        "USUAL     | false | false |              | снег?      | Снега  ",
        "USUAL     | false | false |              | снег*      | Снег   ",
        "USUAL     | false | false |              | снег*      | Снега  ",
        "USUAL     | false | false |              | увага      | ўвага  ",
        "USUAL     | false | false |              | ўвага      | увага  ",
        "EXACT     | false | false |              | сне\u00B4г    | снег   ",
        "EXACT     | false | false |              | снег       | сне\u00B4г ",
        "EXACT     | false | false |              | ўвага      | ўвага  ",
        "EXACT     | true  | true  |              | сь?нег     | сьнег  ",
        "EXACT     | true  | true  |              | сне[гх]     | снег   ",
        "USUAL     | true  | false |              | сьнег      | снег   ",
        "USUAL     | true  | false |              | снег       | сьнег  ",
        "USUAL     | true  | false |              | сьнег      | Снэ\u00B4г ",
        "ALL_FORMS | false | false |              | снег       | снягі  ",
        "ALL_FORMS | true  | false |              | снег       | сьнягі ",
        "GRAMMAR   | true  | false | N.........NP |            | сьнягі ",
        "GRAMMAR   | true  | false | N.........LP |            | снегу  "
    })
    void testManyWordMatches(WordMode mode, boolean variants, boolean regexp, String grammar, String rqWord, String word) {
        assertThat(checkOneWordMatches(mode, variants, regexp, grammar, rqWord, word)).isTrue();
    }

    @ParameterizedTest(name = "Mode {0}: {4} should NOT match {5} (variants={1})")
    @CsvSource(delimiter = '|', value = {
        "USUAL     | false | false |              | снег       | сьнег  ",
        "USUAL     | false | false |              | снег       | снега  ",
        "EXACT     | false | false |              | снег*      | Снега  ",
        "EXACT     | false | false |              | увага      | ўвага  ",
        "EXACT     | false | false |              | ўвага      | увага  ",
        "EXACT     | true  | false |              | снег       | сьнег  ",
        "EXACT     | true  | false |              | снег       | Снег   ",
        "USUAL     | true  | false |              | сьнег      | Сняг   ",
        "ALL_FORMS | false | false |              | снег       | снягіі ",
        "ALL_FORMS | false | false |              | снег       | сьнягі ",
        "GRAMMAR   | false | false | N.........NP |            | сьнягі ",
        "GRAMMAR   | false | false | N.........LP |            | снегу  "
    })
    void testManyWordNonMatches(WordMode mode, boolean variants, boolean regexp, String grammar, String rqWord, String word) {
        assertThat(checkOneWordMatches(mode, variants, regexp, grammar, rqWord, word)).isFalse();
    }

    private boolean checkOneWordMatches(WordRequest.WordMode mode, boolean variants, boolean regexp, String grammar, String rqWord, String word) {
        var bel = LanguageFactory.get("bel");
        var rq = new WordRequest();
        rq.mode = mode;
        rq.variants = variants;
        rq.word = rqWord;
        rq.regexp = regexp;
        rq.grammar = (grammar == null || grammar.isBlank()) ? null : grammar;
        var chain = new ChainRequest();
        chain.words = List.of(rq);
        chain.seps = Arrays.asList(null, null);
        var check = WordsDetailsChecks.createForSearch(a -> false, bel, List.of(chain), false, grFiller);
        var rs = new WordResult(new Word());
        rs.word = word;
        var ti = new TextInfo();
        ti.subtexts = new TextInfo.Subtext[0];
        return check.isOneWordAllowed(ti, rs);
    }

    @Test
    void testMultipleWords() {
        assertThat(checkMultipleWords(List.of(List.of("снег"), List.of("навокал")), true, List.of(List.of("снег"), List.of("навокал")))).isTrue();
        assertThat(checkMultipleWords(List.of(List.of("снег"), List.of("навокал")), false, List.of(List.of("снег", "навокал")))).isTrue();
        assertThat(checkMultipleWords(List.of(List.of("снег"), List.of("навокал")), false, List.of(List.of("снег"), List.of("навокал")))).isFalse();
    }

    private boolean checkMultipleWords(List<List<String>> requires, boolean chainsInParagraph, List<List<String>> text) {
        var bel = LanguageFactory.get("bel");
        var chains = new ArrayList<ChainRequest>();
        for (var ch : requires) {
            var chain = new ChainRequest();
            chain.words = ch.stream().map(w -> {
                var rq = new WordRequest();
                rq.mode = WordMode.USUAL;
                rq.variants = false;
                rq.word = w;
                return rq;
            }).toList();
            chain.seps = new ArrayList<String>();
            chain.seps.add(null);
            ch.forEach(w -> chain.seps.add(null));
            chains.add(chain);
        }

        var check = WordsDetailsChecks.createForSearch(a -> false, bel, chains, chainsInParagraph, grFiller);

        var p = new Paragraph();
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
        var ti = new TextInfo();
        ti.subtexts = new TextInfo.Subtext[0];
        return check.isAllowed(ti, new Paragraph[] { p });
    }

    @Test
    void testWordsAround() {
        assertThat(checkWordsAround(List.of("снег", "навокал"), Arrays.asList("снег", "навокал"))).isTrue();
        assertThat(checkWordsAround(List.of("снег", "навокал"), Arrays.asList("снег", "ляжыць", "навокал"))).isFalse();
        assertThat(checkWordsAround(List.of("снег", "навокал"), Arrays.asList("снег", null, "навокал"))).isTrue();
        assertThat(checkWordsAround(List.of("снег", "навокал"), List.of("навокал", "снег"))).isFalse();
    }

    private boolean checkWordsAround(List<String> ch, List<String> text) {
        var bel = LanguageFactory.get("bel");
        var chain = new ChainRequest();
        chain.words = ch.stream().map(w -> {
            var rq = new WordRequest();
            rq.mode = WordMode.USUAL;
            rq.variants = false;
            rq.word = w;
            return rq;
        }).toList();
        chain.seps = new ArrayList<String>();
        chain.seps.add(null);
        ch.forEach(w -> chain.seps.add(null));

        var check = WordsDetailsChecks.createForSearch(a -> false, bel, List.of(chain), false, grFiller);

        var p = new Paragraph();
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
        var ti = new TextInfo();
        ti.subtexts = new TextInfo.Subtext[0];
        return check.isAllowed(ti, new Paragraph[] { p });
    }

    @Test
    void testSeparators() {
        assertThat(checkSeparators(Arrays.asList(null, "а", "$"), Arrays.asList("б", " ", "а", "."))).isTrue();
        assertThat(checkSeparators(Arrays.asList(null, "а", "."), Arrays.asList("б", " ", "а", "."))).isTrue();
        assertThat(checkSeparators(Arrays.asList(null, "а", ","), Arrays.asList("б", " ", "а", "."))).isFalse();
        assertThat(checkSeparators(Arrays.asList(",", "а", null), Arrays.asList("б", " ", "а", "."))).isFalse();
        assertThat(checkSeparators(Arrays.asList(null, "б", "$"), Arrays.asList("б", " ", "а", "."))).isFalse();
        assertThat(checkSeparators(Arrays.asList("^", "а", null), Arrays.asList("а", " ", "б", "."))).isTrue();
        assertThat(checkSeparators(Arrays.asList("^", "а", ""), Arrays.asList("а", " ", "б", "."))).isTrue();
        assertThat(checkSeparators(Arrays.asList("^", "а", " "), Arrays.asList("а", " ", "б", "."))).isFalse();
        assertThat(checkSeparators(Arrays.asList("^", "а", ""), Arrays.asList(null, "-", "а", " ", "б", "."))).isFalse();
        assertThat(checkSeparators(Arrays.asList("-", "а", ""), Arrays.asList(null, "-", "а", " ", "б", "."))).isTrue();
        assertThat(checkSeparators(Arrays.asList(",", "а", null), Arrays.asList(",б", ", ", "а", "."))).isTrue();
        assertThat(checkSeparators(Arrays.asList(",", "а", null), Arrays.asList(",б", ": ", "а", "."))).isFalse();
    }

    private boolean checkSeparators(List<String> expect, List<String> text) {
        var bel = LanguageFactory.get("bel");
        var chain = new ChainRequest();
        chain.words = new ArrayList<WordRequest>();
        chain.seps = new ArrayList<String>();
        chain.seps.add(expect.get(0));
        for (int i = 1; i < expect.size(); i += 2) {
            var rq = new WordRequest();
            rq.mode = WordMode.USUAL;
            rq.variants = false;
            rq.word = expect.get(i);
            chain.words.add(rq);
            chain.seps.add(expect.get(i + 1));
        }

        var check = WordsDetailsChecks.createForSearch(a -> false, bel, List.of(chain), false, grFiller);

        var p = new Paragraph();
        p.lang = "bel";
        p.sentences = new Sentence[1];
        p.sentences[0] = new Sentence();
        p.sentences[0].words = new WordResult[text.size() / 2];
        for (int i = 0; i < text.size(); i += 2) {
            p.sentences[0].words[i / 2] = new WordResult(new Word());
            p.sentences[0].words[i / 2].word = text.get(i);
            p.sentences[0].words[i / 2].tail = text.get(i + 1);
        }
        var ti = new TextInfo();
        ti.subtexts = new TextInfo.Subtext[0];
        return check.isAllowed(ti, new Paragraph[] { p });
    }
}
