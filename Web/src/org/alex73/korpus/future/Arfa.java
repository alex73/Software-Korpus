package org.alex73.korpus.future;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.belarusian.BelarusianComparators;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.belarusian.FormsReadyFilter;
import org.alex73.korpus.server.KorpusApplication;
import org.alex73.korpus.utils.StressUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/arfa/*" })
public class Arfa extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String word = req.getPathInfo().substring(1);
        word = BelarusianWordNormalizer.lightNormalized(word.trim());
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));
        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        KorpusApplication.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = FormsReadyFilter.getAcceptedForms(p, v);
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
        List<String> list = data.stream().sorted(BelarusianComparators.FULL).collect(Collectors.toList());

        output("future/arfa.html", list, resp);
    }
}
