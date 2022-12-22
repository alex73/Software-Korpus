package org.alex73.korpus.future;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
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
import org.alex73.korpus.languages.belarusian.BelarusianComparators;
import org.alex73.korpus.languages.belarusian.FormsReadyFilter;
import org.alex73.korpus.server.ApplicationOther;
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
        Map<String, PreRow> detailsByKey = new HashMap<>();

        ApplicationOther.instance.gr.getAllParadigms().parallelStream().forEach(p -> {
            for (Variant v : p.getVariant()) {
                List<Form> forms = FormsReadyFilter.getAcceptedForms(FormsReadyFilter.MODE.SHOW, p, v);
                if (forms == null || forms.isEmpty()) {
                    continue;
                }
                for (Form f : forms) {
                    String key = getKey(amoType, f.getValue());
                    if (key == null) {
                        continue; // TODO shouldn't be
                    }
                    KeyDetails newDetail = new KeyDetails(p, v, StressUtils.combineAccute(f.getValue()));
                    synchronized (detailsByKey) {
                        PreRow details = detailsByKey.get(key);
                        if (details == null) {
                            details = new PreRow(key);
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
        Collection<List<PreRow>> prerowgroups = detailsByKey.values().stream().filter(v -> v.details.size() > 1)
                .filter(r -> r.notSimple()).filter(r -> r.onlyLevel(amoType))
                .collect(Collectors.groupingBy(p -> p.getDetails().toString())).values();

        List<Row> data = prerowgroups.stream().map(ps -> new Row(ps)).sorted().distinct().collect(Collectors.toList());

        output("future/amonimy.html", data, resp);
    }

    public static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            int v = BelarusianComparators.FULL.compare(o1, o2);
            if (v == 0) {
                v = o1.compareTo(o2);
            }
            return v;
        }
    };

    private String getKey(AMO_TYPE amoType, String s) {
        switch (amoType) {
        case FORMY:
            return StressUtils.combineAccute(s.toLowerCase());
        case HRAFY:
            return StressUtils.unstress(s.toLowerCase());
        case FONY:
            try {
                return new FanetykaText(ApplicationOther.instance.grFinder, s.replace('+', '´')).ipa;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public static class Row implements Comparable<Row> {
        private final Set<KeyDetails> details;
        private final Set<String> keys = new TreeSet<>(STRING_COMPARATOR);
        private final Set<String> byForms = new TreeSet<>(STRING_COMPARATOR);

        public Row(List<PreRow> prerows) {
            details = prerows.get(0).details;
            for (PreRow pr : prerows) {
                keys.add(pr.key);
                for (KeyDetails ds : pr.details) {
                    byForms.add(ds.byForm);
                }
            }
        }

        public Set<String> getKeys() {
            return keys;
        }

        public Set<KeyDetails> getDetails() {
            return details;
        }

        public Set<String> getByForms() {
            return byForms;
        }

        private String getSortKey() {
            return details.stream().map(d->d.getLemma()).collect(Collectors.joining(" "));
        }
        @Override
        public int compareTo(Row o) {
            return BelarusianComparators.FULL.compare(getSortKey(), o.getSortKey());
        }

        @Override
        public boolean equals(Object obj) {
            Row o = (Row) obj;
            return keys.equals(o.keys) && details.equals(o.details);
        }
    }

    public static class PreRow {
        private final Set<KeyDetails> details = new TreeSet<>();
        private final String key;

        public PreRow(String key) {
            this.key = key;
        }

        public boolean notSimple() {
            String ts = details.stream().map(d -> SetUtils.tag(d.p, d.v).substring(0, 1)).sorted().distinct()
                    .collect(Collectors.joining(""));
            return !"AP".equals(ts);
        }

        public boolean onlyLevel(AMO_TYPE amoType) {
            switch (amoType) {
            case FORMY:
                return true;
            case HRAFY:
            case FONY:
                String kh = null;
                for (KeyDetails d : details) {
                    if (kh == null) {
                        kh = d.byForm.toLowerCase();
                    } else {
                        if (!kh.equalsIgnoreCase(d.byForm)) {
                            return true;
                        }
                    }
                }
                break;
            }
            return false;
        }

        public String getKey() {
            return key;
        }

        public Set<KeyDetails> getDetails() {
            return details;
        }
    }

    public static class KeyDetails implements Comparable<KeyDetails> {
        public final Paradigm p;
        public final Variant v;
        public final String byForm;

        public KeyDetails(Paradigm p, Variant v, String byForm) {
            this.p = p;
            this.v = v;
            this.byForm = byForm;
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
