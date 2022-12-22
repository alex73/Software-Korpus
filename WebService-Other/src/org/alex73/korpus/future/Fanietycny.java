package org.alex73.korpus.future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianComparators;
import org.alex73.korpus.languages.belarusian.FormsReadyFilter;
import org.alex73.korpus.server.ApplicationOther;
import org.alex73.korpus.utils.StressUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/fanietycny/*" })
public class Fanietycny extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String word = req.getPathInfo().substring(1);
        word = LanguageFactory.get("bel").getNormalizer().lightNormalized(word.trim());
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));

        List<Out> result = ApplicationOther.instance.gr.getAllParadigms().parallelStream().flatMap(p -> {
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

        output("future/fanietycny.html", result, resp);
    }

    public static class Out implements Comparable<Out> {
        private final String word;

        public Out(String word) {
            this.word = word;
        }

        public String getWord() {
            return StressUtils.combineAccute(word);
        }

        public String getIpa() {
            return new FanetykaText(ApplicationOther.instance.grFinder, word.replace('+', '´')).ipa;
        }

        public String getSkola() {
            return new FanetykaText(ApplicationOther.instance.grFinder, word.replace('+', '´')).skola;
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
