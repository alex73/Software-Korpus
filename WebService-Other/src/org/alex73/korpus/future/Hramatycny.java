package org.alex73.korpus.future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alex73.grammardb.FormsReadyFilter;
import org.alex73.grammardb.SetUtils;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianComparators;
import org.alex73.korpus.server.ApplicationOther;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/hramatycny/*" })
public class Hramatycny extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String word = req.getPathInfo().substring(1);
        word = LanguageFactory.get("bel").getNormalizer().lightNormalized(word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS);
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));

        List<Out> data = Collections.synchronizedList(new ArrayList<>());
        ApplicationOther.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
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

        output("future/hramatycny.html", data, resp);
    }

    public static class Out implements Comparable<Out> {
        private final String word;
        private final String list;
        private final String grammar;

        public Out(Paradigm p, Variant v, List<Form> forms) {
            this.word = StressUtils.combineAccute(v.getLemma());
            list = new HramatycnyHram(p, v, forms).toString();
            String tag=SetUtils.tag(p, v);
            grammar = String.join(", ",
                    LanguageFactory.get("bel").getTags().describe(tag, ApplicationOther.instance.skipGrammar.get(tag.charAt(0))));
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
