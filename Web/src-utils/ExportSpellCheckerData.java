import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.OfficialSpellFilter;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;
import org.apache.commons.io.FileUtils;

/**
 * Экспартуе звесткі для праверкі правапіса.
 */
public class ExportSpellCheckerData {
    static final Collator BE = Collator.getInstance(new Locale("be"));

    public static void main(String[] args) throws Exception {
        GrammarDB2 db = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");

        Stream<Form> ss = db.getAllParadigms().stream().flatMap(p -> {
            List<Form> result = new ArrayList<>();
            for (Variant v : p.getVariant()) {
                List<Form> fs1 = OfficialSpellFilter.getAcceptedForms(p, v);
                if (fs1 == null) {
                    continue;
                }
                if (!fs1.isEmpty()) {
                    Form f = new Form();
                    f.setValue("");
                    fs1.add(f);
                    result.addAll(fs1);
                }
            }
            return result.stream();
        });

        Stream<String> ss2 = ss.map(f -> f.getValue()).flatMap(s -> {
            if (s.startsWith("у")) {
                String us = "ў" + s.substring(1);
                return Arrays.asList(s, us).stream();
            } else {
                return Arrays.asList(s).stream();
            }
        });
        List<String> list2008 = ss2.collect(Collectors.toList());
        FileUtils.writeLines(new File("slovy-2008-z_naciskami_i_razdialicielami.txt"), "UTF-8", list2008);

        Stream<String> ss3 = db.getAllParadigms().stream().flatMap(p -> {
            List<String> result = new ArrayList<>();
            for (Variant v : p.getVariant()) {
                List<Form> fs1 = OfficialSpellFilter.getAcceptedForms(p, v);
                if (fs1 == null) {
                    continue;
                }
                String tag = SetUtils.tag(p, v);
                for (Form f : fs1) {
                    result.add(StressUtils.unstress(f.getValue()) + "\t" + StressUtils.unstress(v.getLemma()) + "\t"
                            + tag.charAt(0));
                }
            }
            return result.stream();
        });
        Stream<String> ss3u = ss3.flatMap(s -> {
            if (s.startsWith("у")) {
                String us = "ў" + s.substring(1);
                return Arrays.asList(s, us).stream();
            } else {
                return Arrays.asList(s).stream();
            }
        });

        List<String> list2008uniq = ss3u.sorted(BE).distinct().collect(Collectors.toList());
        FileUtils.writeLines(new File("slovy-2008-uniq.txt"), "UTF-8", list2008uniq);
    }
}
