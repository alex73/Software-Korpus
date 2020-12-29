package org.alex73.korpus.future;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.korpus.belarusian.BelarusianComparators;
import org.alex73.korpus.belarusian.OfficialSpellFilter;
import org.alex73.korpus.server.KorpusApplication;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/amonimy/*" })
public class Amonimy extends FutureBaseServlet {

    enum AMO_TYPE {
        FORMY, FONY, HRAFY
    };

    enum AMO_LEVEL {
        FULL, PART, SOME
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] type = req.getPathInfo().substring(1).split("/");
        AMO_TYPE amoType = AMO_TYPE.valueOf(type[0]);
        AMO_LEVEL amoLevel = AMO_LEVEL.valueOf(type[1]);
        Map<String, Row> detailsByKey = new HashMap<>();

        KorpusApplication.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = OfficialSpellFilter.getAcceptedForms(p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : forms) {
                    String key = getKey(amoType, f.getValue());
                    KeyDetails newDetail = new KeyDetails(p, v);
                    synchronized (detailsByKey) {
                        Row details = detailsByKey.get(key);
                        if (details == null) {
                            details = new Row(key);
                            detailsByKey.put(key, details);
                        }
                        details.details.add(newDetail);
                    }
                }
            }
        });
//        Map.Entry<String, Row> max = detailsByKey.entrySet().stream()
//                .collect(Collectors.maxBy((a, b) -> a.getValue().size() - b.getValue().size())).get();
//        System.out.println("ls " + max);
        List<Row> data = detailsByKey.values().stream().filter(v -> v.details.size() > 1).filter(r -> r.notSimple())
                .sorted().distinct().collect(Collectors.toList());

        output("future/amonimy.html", data, resp);
    }

    private String getKey(AMO_TYPE amoType, String s) {
        switch (amoType) {
        case FORMY:
            return s;
        case HRAFY:
            return StressUtils.unstress(s);
        case FONY:
            return new FanetykaText(s.replace('+', '´')).ipa;
        }
        return null;
    }

    public static class Row implements Comparable<Row> {
        private final Set<KeyDetails> details = new TreeSet<>();
        private final String key;

        public Row(String key) {
            this.key = key;
        }

        public boolean notSimple() {
            String ts = details.stream().map(d -> SetUtils.tag(d.p, d.v).substring(0, 1)).sorted().distinct()
                    .collect(Collectors.joining(""));
            return !"AP".equals(ts);
        }

        public String getKey() {
            return key;
        }

        public Set<KeyDetails> getDetails() {
            return details;
        }

        @Override
        public int compareTo(Row o) {
            int c = Integer.compare(o.details.size(), details.size());
            if (c == 0) {
                c = BelarusianComparators.FULL.compare(details.iterator().next().p.getLemma(),
                        o.details.iterator().next().p.getLemma());
            }
            if (c == 0) {
                for (Iterator<KeyDetails> it1 = details.iterator(), it2 = o.details.iterator(); it1.hasNext();) {
                    c = it1.next().compareTo(it2.next());
                    if (c != 0) {
                        break;
                    }
                }
            }
            return c;
        }
    }

    public static class KeyDetails implements Comparable<KeyDetails> {
        public final Paradigm p;
        public final Variant v;

        public KeyDetails(Paradigm p, Variant v) {
            this.p = p;
            this.v = v;
        }

        @Override
        public int compareTo(KeyDetails o) {
            int c = Integer.compare(p.getPdgId(), o.p.getPdgId());
            if (c == 0) {
                c = v.getId().compareTo(o.v.getId());
            }
            return c;
        }

        public String getLemma() {
            return StressUtils.combineAccute(v.getLemma());
        }

        public String getTitle() {
            return p.getMeaning() == null ? "" : p.getMeaning();
        }

        public int getPdgId() {
            return p.getPdgId();
        }

        @Override
        public String toString() {
            String r = p.getPdgId() + v.getId() + "/" + v.getLemma();
            if (p.getMeaning() != null) {
                r += ':' + p.getMeaning();
            }
            r += "/" + SetUtils.tag(p, v);
            return r;
        }
    }
}
