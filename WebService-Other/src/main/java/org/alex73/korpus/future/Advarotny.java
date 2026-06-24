package org.alex73.korpus.future;

import org.alex73.grammardb.FormsReadyFilter;
import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianComparators;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Advarotny extends FutureBaseServlet {
    private final GrammarDB2 grammarDB;

    public Advarotny(GrammarDB2 grammarDB) {
        super("future/advarotny.html");
        this.grammarDB = grammarDB;
    }

    @Override
    public List<String> process(Map<String, String> params) {
        String word = params.get("word");
        word = LanguageFactory.get("bel").getNormalizer().lightNormalized(word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS);
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));
        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        grammarDB.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : forms) {
                    Matcher m = re.matcher(StressUtils.unstress(f.getValue()));
                    if (m.matches()) {
                        data.add(StressUtils.combineAccute(f.getValue()));
                    }
                }
            }
        });
        return data.stream().sorted(BelarusianComparators.FULL_REVERSE).collect(Collectors.toList());
    }
}
