import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarDBSaver;
import org.alex73.korpus.belarusian.FormsReadyFilter;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;
import org.apache.commons.io.FileUtils;

/**
 * Экспартуе звесткі для праверкі правапіса.
 */
public class ExportDatabase {
    static final Collator BE = Collator.getInstance(new Locale("be"));
    static AtomicInteger c = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        GrammarDB2 db = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");

        List<String> list2008 = db.getAllParadigms().stream().flatMap(p -> {
            List<Form> result = new ArrayList<>();
            for (Variant v : p.getVariant()) {
                List<Form> fs1 = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SPELL, p, v);
                if (fs1 == null) {
                    continue;
                }
                if (!fs1.isEmpty()) {
                    c.incrementAndGet();
                    Form f = new Form();
                    f.setValue("");
                    fs1.add(f);
                    result.addAll(fs1);
                }
            }
            return result.stream();
        }).map(f -> f.getValue()).collect(Collectors.toList());
        duplicateU(list2008);
        FileUtils.writeLines(new File("slovy-2008-z_naciskami_i_razdialicielami.txt"), "UTF-8", list2008);

        List<String> list2008uniq = db.getAllParadigms().stream().flatMap(p -> {
            List<String> result = new ArrayList<>();
            for (Variant v : p.getVariant()) {
                List<Form> fs1 = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SPELL, p, v);
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
        }).sorted(BE).distinct().collect(Collectors.toList());
        duplicateU(list2008uniq);
        Collections.sort(list2008uniq, BE);
        FileUtils.writeLines(new File("slovy-2008-uniq.txt"), "UTF-8", list2008uniq);

        db.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                if (v.getForm().isEmpty()) {
                    continue;
                }
                List<Form> fs = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                if (fs != null && !fs.isEmpty()) {
                    v.getForm().clear();
                }
            }
        });
        removeEmpty(db.getAllParadigms());
        FileUtils.deleteDirectory(new File("/data/gits/GrammarDB/noshow/"));
        GrammarDBSaver.sortAndStore(db, "/data/gits/GrammarDB/noshow/");

        db = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");
        db.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> fs = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SPELL, p, v);
                v.getForm().clear();
                if (fs == null) {
                    continue;
                }
                v.getForm().addAll(fs);
            }
        });
        removeEmpty(db.getAllParadigms());
        FileUtils.deleteDirectory(new File("/data/gits/GrammarDB/spell/"));
        GrammarDBSaver.sortAndStore(db, "/data/gits/GrammarDB/spell/");

        db = GrammarDB2.initializeFromDir("/data/gits/GrammarDB");
        db.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> fs = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                v.getForm().clear();
                if (fs == null) {
                    continue;
                }
                v.getForm().addAll(fs);
            }
        });
        removeEmpty(db.getAllParadigms());
        FileUtils.deleteDirectory(new File("/data/gits/GrammarDB/show/"));
        GrammarDBSaver.sortAndStore(db, "/data/gits/GrammarDB/show/");
    }

    static void duplicateU(List<String> words) {
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).startsWith("у")) {
                words.add(i + 1, "ў" + words.get(i).substring(1));
            }
        }
    }

    static void removeEmpty(List<Paradigm> paradigms) {
        for (int i = 0; i < paradigms.size(); i++) {
            Paradigm p = paradigms.get(i);
            for (int j = 0; j < p.getVariant().size(); j++) {
                if (p.getVariant().get(j).getForm().isEmpty()) {
                    p.getVariant().remove(j);
                    j--;
                }
            }
            if (p.getVariant().isEmpty()) {
                paradigms.remove(i);
                i--;
            }
        }
    }
}
