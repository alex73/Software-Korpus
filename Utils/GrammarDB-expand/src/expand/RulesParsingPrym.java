package expand;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormOptions;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.utils.StressUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Правілы для прыметнікаў.
 * Трэба было б перарабіць каб не канструктуяваць па кірылічных тэгах, а каб пераводзіць канфіг у выгляд "тэг формы"->"канчатак"
 */
public class RulesParsingPrym implements IRulesParsing {
    static final Pattern RE_EXAMPLE_PREFIX = Pattern.compile("([A-Z0-9]+):(.+)");
    static final Pattern RE_FORM_PREFIX = Pattern.compile("(\\S)\\s(.+)");
    static final Pattern RE_FORM_MARK = Pattern.compile("(.+)\\((.+)\\)");

    boolean wasError = false;
    Map<String, Rule> rules = new HashMap<>();

    public static void main(String[] args) throws Exception {
        new RulesParsingPrym();
    }

    public void construct(String type, String tag, Variant v) {
        Rule r = rules.get(type);
        if (r == null) {
            throw new RuntimeException("Няма правіла для тыпу " + type + " для " + v.getLemma());
        }

        v.getForm().clear();
        v.getForm().addAll(r.rule.construct(v.getLemma()));
        v.getForm().forEach(f -> {
            f.setTag(tagA(f.getTag()));
        });
    }

    public RulesParsingPrym() throws Exception {
        List<String> lines = FileUtils.readLines(new File("piskunou-rules.txt"), "UTF-8");

        Rule r = new Rule();
        String prefix = "";
        String type = null;
        for (String s : lines) {
            s = fix(s.trim());
            if (s.isEmpty()) {
                continue;
            }
            Matcher m;
            if (s.matches("=+")) {
                if (type == null) {
                    throw new RuntimeException();
                }
                if (!r.example.isEmpty()) {
                    r.parseExample();
                }
                rules.put(type, r);
                r = new Rule();
                type = null;
            } else if (s.matches("\\-+")) {
                if (!r.example.isEmpty()) {
                    r.parseExample();
                }
                r.example.clear();
            } else if (s.startsWith("type:")) {
                type = s.substring(5).trim();
            } else if (s.startsWith("tag:")) {
                r.tag = s.substring(4).trim();
            } else if ((m = RE_EXAMPLE_PREFIX.matcher(s)).matches()) {
                prefix = m.group(1);
                r.example.add(prefix + ':' + m.group(2).trim());
            } else {
                r.example.add(prefix + ':' + s);
            }
        }
        if (type == null) {
            throw new RuntimeException();
        }
        if (!r.example.isEmpty()) {
            r.parseExample();
        }
        rules.put(type, r);

        rules.values().forEach(ru -> ru.createExamples());

        if (wasError) {
            System.out.flush();
            System.err.println("Was error");
        }
    }

    static String fix(String s) {
        return s.replace("ђ", "а´").replace("Ђ", "А´").replace("ј", "е´").replace("ѓ", "о´").replace("ѕ", "і´")
                .replace("ќ", "ы´").replace("њ", "э´").replace("ћ", "я´").replace("џ", "ю´").replace("љ", "у´")
                .replace("Ј", "Е´").replace("Ѓ", "О´").replace("Ѕ", "І´").replace("Ќ", "Ы´").replace("Њ", "Э´")
                .replace("Ћ", "Я´").replace("Џ", "Ю´").replace("Љ", "У´");
    }

    class Rule {
        String tag;
        List<String> example = new ArrayList<>();
        List<RuleWord> parsedExamples = new ArrayList<>();
        RuleWord rule;
        String realBase;

        void parseExample() {
            RuleWord rw = new RuleWord();
            for (String e : example) {
                Matcher m = RE_EXAMPLE_PREFIX.matcher(e);
                if (!m.matches()) {
                    throw new RuntimeException(e);
                }
                for (String ew : m.group(2).split(",")) {
                    ew = ew.trim();
                    if (ew.isEmpty()) {
                        continue;
                    }
                    Matcher m2 = RE_FORM_PREFIX.matcher(ew);
                    if (!m2.matches()) {
                        throw new RuntimeException(e);
                    }
                    for (String ef : m2.group(2).split("\\s+і\\s+")) {
                        ef = ef.trim();
                        rw.parseExample(m.group(1), m2.group(1), ef, realBase);
                    }
                }
            }
            realBase = rw.check();
            if (parsedExamples.isEmpty()) {
                rule = rw;
            }
            parsedExamples.add(rw);

            RuleWord rw0 = parsedExamples.get(0);
            if (rw.forms.size() != rw0.forms.size()) {
                throw new RuntimeException("Розная колькасьць формаў: " + rw.forms + " / " + rw0.forms);
            }
            for (int i = 0; i > rw.forms.size(); i++) {
                RuleForm f0 = rw0.forms.get(i);
                RuleForm f1 = rw.forms.get(i);
                if (!StringUtils.equals(f0.prefix, f1.prefix) || !StringUtils.equals(f0.form, f1.form)
                        || !StringUtils.equals(f0.end, f1.end) || !StringUtils.equals(f0.mark, f1.mark)) {
                    throw new RuntimeException("Прыклады не супадаюць");
                }
            }
        }

        void createExamples() {
            parsedExamples.forEach(w -> rule.compare(w.forms.get(0).base + w.forms.get(0).end, w));
        }
    }

    class RuleWord {
        List<RuleForm> forms = new ArrayList<>();

        void parseExample(String prefix, String form, String word, String realBase) {
            forms.add(new RuleForm(prefix, form, word, realBase));
        }

        String check() {
            String base = getBase(forms.get(0).base + forms.get(0).end);
            for (RuleForm f : forms) {
                String b = f.base;
                if (f.end.startsWith("е") && base.endsWith("д") && b.endsWith("дз")) {
                    // де -> дзе
                    b = b.substring(0, b.length() - 1);
                } else if (f.end.startsWith("е") && base.endsWith("т") && b.endsWith("ц")) {
                    // те -> це
                    b = b.substring(0, b.length() - 1) + "т";
                } else if (f.end.isEmpty() && base.endsWith("в") && b.endsWith("ў")) {
                    // в -> ў
                    b = b.substring(0, b.length() - 1) + "в";
                }
                if (!base.equals(b)) {
                    throw new RuntimeException("Несупадаюць базы слоў у прыкладах : " + base + " і " + b);
                }
            }
            return base;
        }

        void compare(String word, RuleWord other) {
            if (forms.size() != other.forms.size()) {
                throw new RuntimeException("Колькасьць формаў несупадае: " + word);
            }

            String base = getBase(word);

            for (int i = 0; i < forms.size(); i++) {
                RuleForm f = forms.get(i);
                RuleForm of = other.forms.get(i);
                String created = f.create(base);
                String origin = of.base + of.end;
                String err;
                if (!created.replaceAll("[\\(\\)]", "").equals(origin)) {
                    err = "  !!!!!";
                    wasError = true;
                } else {
                    err = "";
                }
                System.out.println(f.prefix + ":" + f.form + (f.mark != null ? "/" + f.mark : "") + " = " + created
                        + " / " + origin + err);
            }
            System.out.println();
        }

        String getBase(String word) {
            String requiredEnd = forms.get(0).end;
            if (!word.endsWith(requiredEnd)) {
                throw new RuntimeException("Канчатак слова " + word + " не такі самы як у прыкладзе "
                        + forms.get(0).base + "|" + forms.get(0).end);
            }
            String base = word.substring(0, word.length() - forms.get(0).end.length());

            if (requiredEnd.startsWith("е") && base.endsWith("дз")) {
                // де -> дзе
                base = base.substring(0, base.length() - 1);
            } else if (requiredEnd.startsWith("е") && base.endsWith("ц")) {
                // те -> це
                base = base.substring(0, base.length() - 1) + "т";
            } else if (requiredEnd.isEmpty() && base.endsWith("ў")) {
                // в -> ў
                base = base.substring(0, base.length() - 1) + "в";
            }
            return base;
        }

        List<Form> construct(String word) {
            String base = getBase(word);

            List<Form> r = new ArrayList<>();
            for (RuleForm f : forms) {
                Form fo = new Form();
                fo.setPravapis("A2008");
                fo.setValue(f.create(base).replace("(", "").replace(")", ""));
                fo.setTag(f.prefix + f.form);
                if (f.mark != null) {
                    fo.setOptions(FormOptions.fromValue(f.mark));
                }
                r.add(fo);
            }
            return r;
        }
    }

    // родны склон множнага ліку: там дзе ня збег зычных - -aў будзе
    // non-standard
    static class RuleForm {
        String prefix;
        String form;
        String realBase;
        String base;
        String end;
        String mark;

        public RuleForm(String prefix, String form, String word, String realBase) {
            Matcher m = RE_FORM_MARK.matcher(word);
            if (m.matches()) {
                word = m.group(1);
                mark = m.group(2);
            }
            word = word.replace('´', '+');
            if (!StressUtils.hasStress(word) && StressUtils.syllCount(word) == 1) {
                word = StressUtils.setStressFromStart(word, 0);
            }
            this.prefix = prefix;
            this.form = form;
            this.realBase = realBase;
            int p = word.indexOf('|');
            if (p >= 0) {
                base = word.substring(0, p);
                end = word.substring(p + 1);
            } else {
                base = word;
                end = "";
            }
        }

        String create(String newBase) {
            if (end.startsWith("е") && newBase.endsWith("д")) {
                // де -> дзе
                return newBase + "(з)" + end;
            } else if (end.startsWith("е") && newBase.endsWith("т")) {
                // те -> це
                return newBase.substring(0, newBase.length() - 1) + "(ц)" + end;
            } else if (end.isEmpty() && newBase.endsWith("в")) {
                // в -> ў
                return newBase.substring(0, newBase.length() - 1) + "(ў)";
            } else {
                return newBase + end;
            }
        }

        @Override
        public String toString() {
            return form;
        }
    }
    static String tagA(String t) {
        switch (t) {
        case "MН":
            return "MNS";
        case "MР":
            return "MGS";
        case "MД":
            return "MDS";
        case "MВ":
            return "MAS";
        case "MТ":
            return "MIS";
        case "MМ":
            return "MLS";
        case "FН":
            return "FNS";
        case "FР":
            return "FGS";
        case "FД":
            return "FDS";
        case "FВ":
            return "FAS";
        case "FТ":
            return "FIS";
        case "FМ":
            return "FLS";
        case "NН":
            return "NNS";
        case "NР":
            return "NGS";
        case "NД":
            return "NDS";
        case "NВ":
            return "NAS";
        case "NТ":
            return "NIS";
        case "NМ":
            return "NLS";
        case "PН":
            return "PNP";
        case "PР":
            return "PGP";
        case "PД":
            return "PDP";
        case "PВ":
            return "PAP";
        case "PТ":
            return "PIP";
        case "PМ":
            return "PLP";
        default:
            throw new RuntimeException(t);
        }
    }
}
