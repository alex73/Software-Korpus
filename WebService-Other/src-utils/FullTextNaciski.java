import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarFinder;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;
import org.alex73.korpus.utils.StressUtils;

public class FullTextNaciski {
    private final static ILanguage.INormalizer NORMALIZER = LanguageFactory.get("bel").getNormalizer();

    static GrammarDB2 gr;
    static GrammarFinder grFinder;

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(args[0]));

        gr = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");
        grFinder = new GrammarFinder(gr);

        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            int prev = 0;
            String o = "";
            while (true) {
                int pb = s.indexOf("<b>", prev);
                if (pb < 0) {
                    o += s.substring(prev);
                    break;
                }
                o += s.substring(prev, pb);
                int pe = s.indexOf("</b>", pb);
                if (pe < 0) {
                    throw new Exception(s);
                }
                pe += 4;
                o += naciski(s.substring(pb, pe));
                prev = pe;
            }
            lines.set(i, o);
        }

        Files.write(Path.of(args[0]), lines);
    }

    static String naciski(String s) {
        Splitter3 splitter = new Splitter3(LanguageFactory.get("bel").getNormalizer(), false, new IProcess() {
            @Override
            public synchronized void showStatus(String status) {
            }

            @Override
            public synchronized void reportError(String error, Throwable ex) {
                throw new RuntimeException(ex);
            }
        });
        TextLine line = splitter.parse(s);
        StringBuilder out = new StringBuilder();
        for (ITextLineElement el : line) {
            if (el instanceof WordItem w) {
                String word = StressUtils.unstress(w.word);
                out.append(wordAccent(word));
            } else if (el instanceof SentenceSeparatorItem) {
            } else {
                out.append(el.getText());
            }
        }
        return out.toString();
    }

    static String wordAccent(String word) {
        if (StressUtils.syllCount(word) < 2) {
            return word;
        }
        String wNormalized = NORMALIZER.lightNormalized(word, ILanguage.INormalizer.PRESERVE_NONE);
        Set<Integer> stresses = new TreeSet<>();
        for (Paradigm p : grFinder.getParadigms(word)) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (f.getValue().isEmpty()) {
                        continue;
                    }
                    if (NORMALIZER.lightNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE).equals(wNormalized)) {
                        stresses.addAll(StressUtils.getAllStressesFromEnd(f.getValue()));
                    }
                }
            }
        }
        for (int pos : stresses) {
            word = StressUtils.setStressFromEnd(word, pos);
        }
        if (stresses.size() != 1) {
            word = "=" + word;
        }
        return word;
    }
}
