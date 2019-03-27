package expand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormOptions;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.utils.StressUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class RulesParsingDziejaslovy implements IRulesParsing {
    static final Pattern RE_EXAMPLE_PREFIX = Pattern.compile("([A-Z0-9]+):(.+)");
    static final Pattern RE_FORM_PREFIX = Pattern.compile("(\\S)\\s(.+)");
    static final Pattern RE_FORM_MARK = Pattern.compile("(.+)\\((.+)\\)");

    boolean wasError = false;
    List<Rule> rules = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new RulesParsingDziejaslovy();
    }

    public void construct(String type, String tag, Variant v) {
        Rule r = null;
        for(Rule rule:rules) {
            if (type.equals(rule.type) && rule.tag.matcher(tag).matches()) {
                String requiredEnd = rule.rule.forms.get(0).end;
                if (!v.getLemma().endsWith(requiredEnd)) {
                    continue;
                }
                if (r!=null) {
                    throw new RuntimeException();
                }
                r=rule;
            }
        }
        if (r == null) {
            throw new RuntimeException("Няма правіла для тыпу " + type + " для " + v.getLemma());
        }

        v.getForm().clear();
        v.getForm().addAll(r.rule.construct(v.getLemma()));
    }

    public RulesParsingDziejaslovy() throws Exception {
        List<String> lines = FileUtils.readLines(new File("piskunou-rules-V.txt"), "UTF-8");

        Rule r = new Rule();
        for (String s : lines) {
            s = fix(s.trim());
            if (s.isEmpty()) {
                continue;
            }
            Matcher m;
            if (s.matches("=+")) {
                if (r.example.isEmpty()) {
                    throw new RuntimeException();
                }
                if (!r.example.isEmpty()) {
                    r.parseExample();
                }
                rules.add(r);
                r = new Rule();
                r.type = null;
            } else if (s.matches("\\-+")) {
                if (!r.example.isEmpty()) {
                    r.parseExample();
                }
                r.example.clear();
            } else if (s.startsWith("type:")) {
                r.type = s.substring(5).trim();
            } else if (s.startsWith("tag:")) {
                r.tag = Pattern.compile(s.substring(4).trim());
            } else if ((m = RE_EXAMPLE_PREFIX.matcher(s)).matches()) {
                r.example.add(s);
            } else {
                throw new RuntimeException("Wrong line: "+s);
            }
        }
        if (!r.example.isEmpty()) {
            r.parseExample();
        }
        rules.add(r);

        rules.forEach(ru -> ru.createExamples());

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
        String type;
        Pattern tag;
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
                rw.parseExample(m.group(1), m.group(2));
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
                if (StringUtils.equals(f0.form, f1.form)
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

        void parseExample(String form, String word) {
            forms.add(new RuleForm(form, word));
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
                System.out.println(f.form + (f.mark != null ? "/" + f.mark : "") + " = " + created
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
                fo.setTag(f.form);
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
        String form;
        String base;
        String end;
        String mark;

        public RuleForm( String form, String word) {
            Matcher m = RE_FORM_MARK.matcher(word);
            if (m.matches()) {
                word = m.group(1);
                mark = m.group(2);
            }
            word = word.replace('´', '+');
            if (!StressUtils.hasStress(word) && StressUtils.syllCount(word) == 1) {
                word = StressUtils.setStressFromStart(word, 0);
            }
            this.form = form;
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
}
