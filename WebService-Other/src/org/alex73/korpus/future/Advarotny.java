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

import org.alex73.grammardb.FormsReadyFilter;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianComparators;
import org.alex73.korpus.server.ApplicationOther;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/advarotny/*" })
public class Advarotny extends FutureBaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String word = req.getPathInfo().substring(1);
        word = LanguageFactory.get("bel").getNormalizer().lightNormalized(word.trim(), ILanguage.INormalizer.PRESERVE_WILDCARDS);
        Pattern re = Pattern.compile(word.replace("+", "").replace("*", ".*").replace('?', '.'));
        Set<String> data = Collections.synchronizedSet(new HashSet<>());
        ApplicationOther.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
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
        List<String> list = data.stream().sorted(BelarusianComparators.FULL_REVERSE).collect(Collectors.toList());

        output("future/advarotny.html", list, resp);
    }
}
