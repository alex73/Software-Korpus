package org.alex73.korpus.future;

import org.alex73.grammardb.FormsReadyFilter;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.SetUtils;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianComparators;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hramatycny extends FutureBaseServlet {
    private final GrammarDB2 grammarDB;
    private final Map<Character, Set<String>> skipGrammar;

    public Hramatycny(GrammarDB2 grammarDB, Map<Character, Set<String>> skipGrammar) {
        super("future/hramatycny.html");
        this.grammarDB = grammarDB;
        this.skipGrammar = skipGrammar;
    }

    @Override
    public List<Out> process(Map<String, String> params) {
        String word = params.get("word");
        word = LanguageFactory.get("bel").getNormalizer().lightNormalized(word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS);
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));

        List<Out> data = Collections.synchronizedList(new ArrayList<>());
        grammarDB.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : v.getForm()) {
                    Matcher m = re.matcher(StressUtils.unstress(f.getValue()));
                    if (m.matches()) {
                        data.add(new Out(p, v, forms));
                        break;
                    }
                }
            }
        });
        Collections.sort(data);

        return data;
    }

    public class Out implements Comparable<Out> {
        private final String word;
        private final String list;
        private final String grammar;

        public Out(Paradigm p, Variant v, List<Form> forms) {
            this.word = StressUtils.combineAccute(v.getLemma());
            list = new HramatycnyHram(p, v, forms).toString();
            String tag = SetUtils.tag(p, v);
            grammar = String.join(", ",
                    LanguageFactory.get("bel").getTags().describe(tag, skipGrammar.get(tag.charAt(0))));
        }

        public String getWord() {
            return word;
        }

        public String getList() {
            return list;
        }

        public String getGrammar() {
            return grammar;
        }

        @Override
        public int compareTo(Out o) {
            return BelarusianComparators.FULL.compare(word, o.word);
        }
    }
}
