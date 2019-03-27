package expand;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormOptions;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Slounik;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.GrammarDB2;
import org.alex73.korpus.base.GrammarDBSaver;
import org.alex73.korpus.utils.StressUtils;

public class ExpandProcess {
    static final String GRAMMAR_DB_PATH = "../../../GrammarDB/";
    static List<String> errors = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int r = new ProcessBuilder("git", "reset", "--hard").directory(new File(GRAMMAR_DB_PATH)).start().waitFor();
        if (r != 0) {
            System.err.println("Error execute git reset");
            System.exit(1);
        }

        // process("V", new RulesParsingDziejaslovy());
        process("A1", new RulesParsingPrym());
        // process("A2", new RulesParsingPrym());
        // process("P", new RulesParsingPrym());
        // processNew("A_", new RulesParsingPrym());
        // processNew("P_", new RulesParsingPrym());
    }

    static void check() throws Exception {
        List<String> nn = new ArrayList<>();
        GrammarDB2 db = GrammarDB2.initializeFromFile(new File("/data/gits/GrammarDB/A1.xml"));
        // GrammarDB2 db = GrammarDB2.initializeFromDir(".");
        for (Paradigm p : db.getAllParadigms()) {
            for (Variant v : p.getVariant()) {
                if (v.getSlounik().isEmpty()) {
                    continue;
                }
                for (Form f : v.getForm()) {
                    if (!StressUtils.hasStress(f.getValue())) {
                        nn.add(f.getValue());
                    }
                }
            }
        }
        Files.write(Paths.get("/tmp/log-nn.log"), nn);
    }

    static void processNew(String ft, IRulesParsing rules) throws Exception {
        GrammarDB2 db = GrammarDB2.initializeFromFile(new File(GRAMMAR_DB_PATH + ft + ".xml"));
        GrammarDBSaver.store(db.getAllParadigms(), new File("/tmp/orig-" + ft + ".xml"));
        errors.clear();

        for (Paradigm p : db.getAllParadigms()) {
            for (Variant v : p.getVariant()) {
                String slp = getSlounikPiskunou(v);
                if (slp == null) {
                    continue;
                }
                String[] cl = slp.split("///");
                if (cl.length != 2) {
                    throw new Exception();
                }
                cl[1] = cl[1].replaceAll("\\s*\\[(.+)\\]\\s*", "$1").trim();
                if (!cl[1].startsWith("ad")) {
                    e(v.getLemma() + ": без пазнакі тыпу: " + slp);
                    continue;
                }
                try {
                    rules.construct(cl[1], p.getTag(), v);
                } catch (Exception ex) {
                    e(ex.getMessage() + " : " + ex);
                }
                Collections.sort(v.getForm(), GrammarDBSaver.COMPARATOR_FORM_PRYM);
            }
        }

        GrammarDBSaver.store(db.getAllParadigms(), new File(GRAMMAR_DB_PATH + ft + ".xml"));
        Collections.sort(errors, Collator.getInstance(new Locale("be")));
        Files.write(Paths.get("log-" + ft + ".log"), errors);
    }

    static void process(String ft, IRulesParsing rules) throws Exception {
        GrammarDB2 db = GrammarDB2.initializeFromFile(new File(GRAMMAR_DB_PATH + ft + ".xml"));
        GrammarDBSaver.store(db.getAllParadigms(), new File("/tmp/orig-" + ft + ".xml"));
        errors.clear();

        for (Paradigm p : db.getAllParadigms()) {
            for (Variant v : p.getVariant()) {
                if (!StressUtils.hasStress(v.getLemma())) {
                    // e("Пакуль няма націску ў " + p.getPdgId() + v.getId() + "/" + v.getLemma());
                }
                String slp = getSlounikPiskunou(v);
                if (slp == null) {
                    continue;
                }
                if (!StressUtils.hasStress(v.getLemma())) {
                    e("Пакуль няма націску, хоць ёсць слоўнік Піскунова " + p.getPdgId() + v.getId() + "/"
                            + v.getLemma());
                    String piskunouWord = slp.replaceAll("^\\{b\\}([^\\{]+)\\{/b\\}.+$", "$1").replace('´', '+')
                            .replace('’', '\'');
                    if (!piskunouWord.equals(v.getLemma())) {
                        if (onlyStressDifferent(piskunouWord, v.getLemma())) {
                            v.setLemma(piskunouWord);
                            if (v == p.getVariant().get(0)) {
                                p.setLemma(piskunouWord);
                            }
                        } else {
                            e("Несупадаюць націскі: p:" + piskunouWord + " " + p.getPdgId() + v.getId() + "/"
                                    + v.getLemma());
                        }
                    }
                }
                String[] cl = slp.split("///");
                if (cl.length != 2) {
                    throw new Exception();
                }
                String piskunouType = cl[1].replaceAll("\\s*\\[(.+)\\]\\s*", "$1").trim();
                /*
                 * TODO if (v.getPravapis() == null || !v.getPravapis().equals("A2008")) {
                 * e(v.getLemma() + ": няправільная пазнака правапісу: " + v.getPravapis() +
                 * " для " + v.getLemma()); }
                 */
                try {
                    Variant v2 = new Variant();
                    v2.setLemma(v.getLemma());
                    rules.construct(piskunouType, p.getTag(), v2);
                    v2.getForm().get(0).setSlouniki("piskunou2012");
                    pieranos(v2, v);
                } catch (Exception ex) {
                    e(ex.getMessage() + " : " + ex);
                }
                Collections.sort(v.getForm(), GrammarDBSaver.COMPARATOR_FORM_PRYM);
            }
        }

        GrammarDBSaver.store(db.getAllParadigms(), new File(GRAMMAR_DB_PATH + ft + ".xml"));
        Collections.sort(errors, Collator.getInstance(new Locale("be")));
        Files.write(Paths.get("log-" + ft + ".log"), errors);
    }

    static void e(String err) {
        errors.add(err);
        System.err.println(err);
    }

    static String getSlounikPiskunou(Variant v) {
        for (Slounik s : v.getSlounik()) {
            if ("piskunou2012".equals(s.getName())) {
                return s.getValue();
            }
        }
        return null;
    }

    static void pieranos(Variant vconstructed, Variant vdb) {
        Map<String, Form> formyTagValue = new TreeMap<>();
        for (Form f : vdb.getForm()) {
            if (formyTagValue.put(f.getTag() + "/" + f.getValue(), f) != null) {
                e("Паўтараецца форма ў " + vdb.getLemma() + ": " + f.getTag() + "/" + f.getValue());
            }
        }
        for (Form f : vconstructed.getForm()) {
            String ftx = 'X' + f.getTag().substring(1);
            Form fdb = formyTagValue.get(f.getTag() + "/" + f.getValue());
            if (fdb == null) {
                fdb = formyTagValue.get(f.getTag() + "/" + StressUtils.unstress(f.getValue()));
            }
            if (fdb == null) {
                fdb = formyTagValue.get(ftx + "/" + f.getValue());
            }
            if (fdb == null) {
                fdb = formyTagValue.get(ftx + "/" + StressUtils.unstress(f.getValue()));
            }
            if (fdb == null) {
                fdb = constructFormByOther(vdb.getLemma(), f, formyTagValue);
                if (fdb != null) {
                    vdb.getForm().add(f);
                }
            }
            if (fdb == null) {
                continue;
            }
            if (f.getOptions() == null && fdb.getOptions() != null) {
                e("Options зменена на пустую для " + vdb.getLemma() + ": " + f.getTag() + "/" + f.getValue());
            } else if (f.getOptions() != null && fdb.getOptions() != null) {
                if (f.getOptions() != fdb.getOptions()) {
                    e("Options несупадае для " + vdb.getLemma() + ": " + f.getTag() + "/" + f.getValue());
                }
            } else if (f.getOptions() == null && fdb.getOptions() == null) {
                fdb.setValue(f.getValue());
                fdb.setTag(f.getTag());
            } else {
                fdb.setOptions(f.getOptions());
                fdb.setValue(f.getValue());
                fdb.setTag(f.getTag());
            }
            validatePrym(f, fdb, formyTagValue);

            if (!StressUtils.hasStress(fdb.getValue())) {
                if (StressUtils.unstress(f.getValue()).equals(fdb.getValue())) {
                    fdb.setValue(f.getValue());
                } else {
                    e("Несупадаюць формы: " + vdb.getLemma() + ": " + f.getTag() + "/" + f.getValue() + " -- "
                            + fdb.getValue());
                }
            }
        }
    }

    static Form constructFormByOther(String lemma, Form f, Map<String, Form> formyTagValue) {
        /*
         * Калі няма PAP - дадаваць з anim/inanim абавязкова Калі супадае з PGP - дадаем
         * правапіс
         *
         * Калі няма MAS - дадаваць з anim/inanim абавязкова Калі супадае з MGS - дадаем
         * правапіс
         */
        if (f.getTag().equals("PAP")) {
            String of;
            if (f.getOptions() == FormOptions.ANIM) {
                of = "PGP";
            } else if (f.getOptions() == FormOptions.INANIM) {
                of = "PNP";
            } else {
                e("Няма Options для :" + f.getTag() + "/" + f.getValue());
                return null;
            }
            Form fPGP = formyTagValue.get(of + "/" + f.getValue());
            if (fPGP == null) {
                e("Няма сканструяванай формы/" + of + ": " + lemma + ": " + f.getTag() + "/" + f.getValue());
                return null;
            } else if (!fPGP.getValue().equals(f.getValue())) {
                e("Несупадаюць формы: PAP=" + f.getValue() + " " + of + "=" + fPGP.getValue());
                return null;
            } else {
                f.setPravapis(fPGP.getPravapis());
                return f;
            }
        } else if (f.getTag().equals("MAS")) {
            String of;
            if (f.getOptions() == FormOptions.ANIM) {
                of = "MGS";
            } else if (f.getOptions() == FormOptions.INANIM) {
                of = "MNS";
            } else {
                e("Няма Options для :" + f.getTag() + "/" + f.getValue());
                return null;
            }
            Form fMGS = formyTagValue.get(of + "/" + f.getValue());
            if (fMGS == null) {
                e("Няма сканструяванай формы/" + of + ": " + lemma + ": " + f.getTag() + "/" + f.getValue());
                return null;
            } else if (!fMGS.getValue().equals(f.getValue())) {
                e("Несупадаюць формы: MAS=" + f.getValue() + " " + of + "=" + fMGS.getValue());
                return null;
            } else {
                f.setPravapis(fMGS.getPravapis());
                return f;
            }
        } else {
            if (f.getTag().equals("FGS") && f.getValue().endsWith("е")) {
                Form foth = formyTagValue.get("FGS/" + f.getValue().replaceAll("е$", "й"));
                if (foth == null) {
                    e("Няма сканструяванай формы/FGS : " + lemma + ": " + f.getTag() + "/" + f.getValue());
                    return null;
                } else {
                    f.setPravapis(foth.getPravapis());
                    return f;
                }
            } else {
                e("Няма сканструяванай формы : " + lemma + ": " + f.getTag() + "/" + f.getValue());
                return null;
            }
        }
    }

    static void validatePrym(Form f, Form fdb, Map<String, Form> formyTagValue) {
        if ("PAP".equals(fdb.getTag()) && fdb.getPravapis() == null) {
            String of;
            if (f.getOptions() == FormOptions.ANIM) {
                of = "PGP";
            } else if (f.getOptions() == FormOptions.INANIM) {
                of = "PNP";
            } else {
                e("Няма Options для :" + f.getTag() + "/" + f.getValue());
                return;
            }
            Form fPGP = formyTagValue.get(of + "/" + f.getValue());
            if (fPGP != null) {
                fdb.setPravapis(fPGP.getPravapis());
            }
        } else if ("MAS".equals(fdb.getTag()) && fdb.getPravapis() == null) {
            String of;
            if (f.getOptions() == FormOptions.ANIM) {
                of = "MGS";
            } else if (f.getOptions() == FormOptions.INANIM) {
                of = "MNS";
            } else {
                e("Няма Options для :" + f.getTag() + "/" + f.getValue());
                return;
            }
            Form fPGP = formyTagValue.get(of + "/" + f.getValue());
            if (fPGP != null) {
                fdb.setPravapis(fPGP.getPravapis());
            }
        }
    }

    static boolean onlyStressDifferent(String stressed, String unstressed) {
        String[] sm = stressed.split("\\-");
        String[] um = unstressed.split("\\-");
        if (sm.length != um.length) {
            return false;
        }
        for (int i = 0; i < sm.length; i++) {
            if (!StressUtils.unstress(sm[i]).equals(um[i])) {
                return false;
            }
            if (sm[i].length() - 1 != um[i].length()) {
                return false;
            }
        }
        return true;
    }
}
