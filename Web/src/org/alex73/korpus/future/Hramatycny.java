package org.alex73.korpus.future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.alex73.korpus.base.BelarusianComparators;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.base.OfficialSpellFilter;
import org.alex73.korpus.server.KorpusApplication;
import org.alex73.korpus.utils.StressUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/hramatycny/*" })
public class Hramatycny extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String word = req.getPathInfo().substring(1);
        word = BelarusianWordNormalizer.lightNormalized(word.trim());
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));

        List<String> data = Collections.synchronizedList(new ArrayList<>());
        KorpusApplication.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = OfficialSpellFilter.getAcceptedForms(p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : v.getForm()) {
                    Matcher m = re.matcher(StressUtils.unstress(f.getValue()));
                    if (m.matches()) {
                        String line = forms.stream().map(fo -> StressUtils.combineAccute(fo.getValue()))
                                .collect(Collectors.joining(", "));
                        data.add("<tr><td>" + StressUtils.combineAccute(v.getLemma()) + "</td><td>" + line
                                + "</td></tr>");
                        break;
                    }
                }
            }
        });
        Collections.sort(data, BelarusianComparators.FULL);

        output("future/hramatycny.html", data, resp);
    }
}
