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

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.korpus.base.BelarusianComparators;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.OfficialSpellFilter;
import org.alex73.korpus.server.KorpusApplication;
import org.alex73.korpus.utils.StressUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/fanietycny/*" })
public class Fanietycny extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String word = req.getPathInfo().substring(1);
        word = BelarusianWordNormalizer.lightNormalized(word.trim());
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));

        List<Pair> data = Collections.synchronizedList(new ArrayList<>());
        KorpusApplication.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = OfficialSpellFilter.getAcceptedForms(p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : v.getForm()) {
                    Matcher m = re.matcher(StressUtils.unstress(f.getValue()));
                    if (m.matches()) {
                        data.add(new Pair(f.getValue()));
                    }
                }
            }
        });
        Collections.sort(data);

        output("future/fanietycny.html", data, resp);
    }

    public static class Pair implements Comparable<Pair> {
        private final String word;

        public Pair(String word) {
            this.word = StressUtils.combineAccute(word);
        }

        public String getWord() {
            return word;
        }

        public String getFanietyka() {
            return new FanetykaText(word.replace('+', '´')).ipa;
        }

        @Override
        public int compareTo(Pair o) {
            return BelarusianComparators.FULL.compare(word, o.word);
        }
    }
}
