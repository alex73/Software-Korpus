package org.alex73.korpus.future;

import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.grammardb.FormsReadyFilter;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianComparators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Fanietycny extends FutureBaseServlet {

    private final GrammarDB2 grammarDB;
    private final GrammarFinder grFinder;

    public Fanietycny(GrammarDB2 grammarDB, GrammarFinder grFinder) {
        super("future/fanietycny.html");
        this.grammarDB = grammarDB;
        this.grFinder = grFinder;
    }

    @Override
    public List<Out> process(Map<String, String> params) {
        String word = params.get("word");
        word = LanguageFactory.get("bel").getNormalizer().lightNormalized(word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS);
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));

        return grammarDB.getAllParadigms().parallelStream().flatMap(p -> {
            List<Out> data = new ArrayList<>();
            for (Variant v : p.getVariant()) {
                List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : v.getForm()) {
                    Matcher m = re.matcher(StressUtils.unstress(f.getValue()));
                    if (m.matches()) {
                        data.add(new Out(f.getValue()));
                    }
                }
            }
            return data.stream();
        }).sequential().sorted().distinct().collect(Collectors.toList());
    }

    public class Out implements Comparable<Out> {
        private final String word;

        public Out(String word) {
            this.word = word;
        }

        public String getWord() {
            return StressUtils.combineAccute(word);
        }

        public String getIpa() throws Exception {
            return new FanetykaText(grFinder, word.replace('+', '´')).ipa;
        }

        public String getSkola() throws Exception {
            return new FanetykaText(grFinder, word.replace('+', '´')).skola;
        }

        @Override
        public int compareTo(Out o) {
            return BelarusianComparators.FULL.compare(word, o.word);
        }

        @Override
        public boolean equals(Object obj) {
            Out o = (Out) obj;
            return word.equals(o.word);
        }
    }
}
