package org.alex73.korpus.compiler;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.xml.XMLConstants;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.alex73.korpus.base.BelarusianTags;
import org.alex73.korpus.editor.core.GrammarDB;
import org.alex73.korpus.utils.StressUtils;
import org.apache.commons.io.FileUtils;

import alex73.corpus.paradigm.Paradigm;
import alex73.corpus.paradigm.Paradigm.Form;
import alex73.corpus.paradigm.Type;
import alex73.corpus.paradigm.Wordlist;


/**
 * “Правільныя” памылкі:
0. Няправільныя сімвалы ў VIMR1/вакуумавацца: вакуумавацца
0. Няправільныя сімвалы ў VXMN1/вакуумава´ць: вакуумава´ць
0. Няправільныя сімвалы ў VXMN1/лакаутава´ць: лакаутава´ць
0. Няправільныя сімвалы ў VXMN1/трыумфава´ць: трыумфава´ць
0. Няправільныя сімвалы ў VXPN1/вакуумава´ць: вакуумава´ць
0. Няправільныя сімвалы ў VXPN1/лакаутава´ць: лакаутава´ць
0. Паўтараецца tag+lemma: VIMR1/клікацца
0. Паўтараецца tag+lemma: VIMR1/слацца
0. Паўтараецца tag+lemma: VIPR1/заткну´цца
0. Паўтараецца tag+lemma: VIPR1/пераспявацца
0. Паўтараецца tag+lemma: VIPR2/разрадзіцца
0. Паўтараецца tag+lemma: VXMN1/клі´каць
0. Паўтараецца tag+lemma: VXMN1/скіса´ць
0. Паўтараецца tag+lemma: VXMN1/слаць
0. Паўтараецца tag+lemma: VXMR1/прасыпа´цца
0. Паўтараецца tag+lemma: VXPN1/вы´жаць
0. Паўтараецца tag+lemma: VXPN1/вы´слаць
0. Паўтараецца tag+lemma: VXPN1/пасла´ць
0. Паўтараецца tag+lemma: VXPN2/асвяці´ць
0. Паўтараецца tag+lemma: VXPN2/зматлашы´ць
5. Спражэньне няправільна пазначана: VXPN3/даць

TODO: назоўнікі і прыметнікі - адушаўлёнасць
 */
public class CheckGrammarDB {

    static List<Paradigm> paradigms = new ArrayList<>();
    static List<String> errors = new ArrayList<>();
    static int maxPdgId = 0;
    static Validator validator;

    public static void main(String[] args) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(CheckGrammarDB.class.getResourceAsStream("/xsd/Paradigm.xsd")));
        validator = schema.newValidator();

        // дзеясловы
        read(new File("GrammarDB").listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".xml") && !f.getName().equals("clusters.xml");
            }
        }));

        checkUniques();
        for (Paradigm p : paradigms) {
            if ("бы´ць".equals(p.getLemma())) {
                continue;
            }
            checkUniqueForms(p);
            checkSlounikPravapis(p);
            check2(p);
            check3(p);
            check4(p);
            if (p.getTag().startsWith("V")) {
                checkV1(p);
                checkV4(p);
                checkV5(p);
                checkV67(p);
                // пакуль выключым checkV8(p);
            }
        }

        System.out.println("Max pdgId=" + maxPdgId);
        errors = new ArrayList(new TreeSet<String>(errors));
        if (!errors.isEmpty()) {
            Collections.sort(errors);
            File out = new File("GrammarDB/errors.txt");
            FileUtils.writeLines(out, errors, "\r\n");
            JOptionPane.showMessageDialog(null, errors.size() + " памылак у " + out.getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(null, "Памылак няма");
        }
    }

    static void read(File[] grammarFiles) throws Exception {
        for (File f : grammarFiles) {
            System.out.println("Read " + f);
            try {
                validator.validate(new StreamSource(f));
                Unmarshaller unm = GrammarDB.CONTEXT.createUnmarshaller();
                Wordlist wordlist = (Wordlist) unm.unmarshal(f);
                paradigms.addAll(wordlist.getParadigm());
            } catch (Exception ex) {
                errors.add("0. Памылка чытаньня " + f + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Правяраем унікальнасьць pdgId і tag+lemma.
     */
    static void checkUniques() {
        Set<Integer> uniqPdgId = new HashSet<>(paradigms.size());
        Set<String> uniqTagLemma = new HashSet<>(paradigms.size());
        for (Paradigm p : paradigms) {
            maxPdgId = Math.max(maxPdgId, p.getPdgId());
            if (!uniqPdgId.add(p.getPdgId())) {
                errors.add("1. Паўтараецца pdgId=" + p.getPdgId());
            }

            if (p.getType() == null) {
                String tl = p.getTag() + '/' + p.getLemma();
                if (!uniqTagLemma.add(tl)) { // TODO націскі
                    errors.add("1. Паўтараецца tag+lemma: " + p(p));
                }
            }
        }
    }

    /**
     * Правяраем унікальнасьць форм.
     */
    static void checkUniqueForms(Paradigm p) {
        Set<String> u = new HashSet<>();
        for (Form f : p.getForm()) {
            if (f.getType() == Type.NONSTANDARD || f.getType() == Type.POTENTIAL
                    || f.getType() == Type.VARIANT) {
                continue;
            }
            if (!u.add(f.getTag() + '/' + f.getType())) {
                errors.add("1. Паўтараюцца формы ў " + p(p) + ": " + f.getTag());
                return;
            }
        }
    }
    
    static Set<String> KNOWN_SLOUNIKI = new HashSet<>(Arrays.asList("sbm2012", "prym2009", "dzsl2007", "nazounik2008"));
    static Set<String> KNOWN_PRAVAPIS = new HashSet<>(Arrays.asList("A1957", "A2008"));

    static void checkSlounikPravapis(Paradigm p) {
        for (Form f : p.getForm()) {
            if (hasUnknown(KNOWN_SLOUNIKI, f.getSlouniki())) {
                    errors.add("2. Невядомыя слоўнікі ў " + p(p) + ": "+f.getSlouniki());
            }
            if (hasUnknown(KNOWN_PRAVAPIS, f.getPravapis())) {
                    errors.add("2. Невядомыя правапісы ў " + p(p) + ": "+f.getPravapis());
            }
        }
    }
    static boolean hasUnknown(Set<String> known, String list) {
        if (list==null) {
            return false;
        }
        for(String w:list.split(",")) {
            if (!known.contains(w.trim())) {
                return true;
            }
        }
        return false;
    }

    static void check2(Paradigm p) {
        if (!isWordValid(p.getLemma())) {
            errors.add("2. Няправільныя сімвалы ў " + p(p) + ": " + p.getLemma());
            return;
        }
        for (Form f : p.getForm()) {
            if (!isWordValid(f.getValue())) {
                errors.add("2. Няправільныя сімвалы ў " + p(p) + "/" + f.getTag() + ": " + f.getValue());
                return;
            }
        }
    }

    static void check3(Paradigm p) {
        try {
            StressUtils.checkStress(p.getLemma());
        } catch (Exception ex) {
            errors.add("3. Няправільны націск у " + p(p) + ": " + ex.getMessage());
        }
        if (StressUtils.syllCount(p.getLemma()) < 1) {
            errors.add("3. Няма галосных у " + p(p));
        }
        for (Form f : p.getForm()) {
            try {
                StressUtils.checkStress(f.getValue());
            } catch (Exception ex) {
                errors.add("3. Няправільны націск у " + p(p) + "/" + f.getValue() + ": " + ex.getMessage());
            }
            if (!f.getValue().isEmpty() && StressUtils.syllCount(f.getValue()) < 1) {
                errors.add("3. Няма галосных у " + p(p) + ": " + f.getValue());
            }
        }
    }

    static void check4(Paradigm p) {
        for (Form f : p.getForm()) {
            if (!BelarusianTags.getInstance().isValid(p.getTag() + f.getTag(), null)) {
                errors.add("4. Няправільны тэг " + p.getTag() + f.getTag() + ": " + p(p));
            }
        }
    }

    static final String letters = "ёйцукенгшўзх'фывапролджэячсмітьбю" + StressUtils.STRESS_CHAR;
    static final String galosnyja = "ёуеыаоэяію";

    static boolean isWordValid(String word) {
        char prev = ' ';
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (letters.indexOf(c) < 0) {
                return false;
            }
            if (c == 'у' && galosnyja.indexOf(prev) >= 0) {
                return false;
            }
            if (c == 'ў' && galosnyja.indexOf(prev) < 0) {
                return false;
            }
            if (c != StressUtils.STRESS_CHAR) {
                prev = c;
            }
        }
        return true;
    }

    /**
     * Правяраем супадзеньне леммы і 0 формы.
     */
    static void checkV1(Paradigm p) {
        Form f = getForm(p, "0");
        if (f == null) {
            return;
        }
        if (!p.getLemma().equals(f.getValue())) {
            errors.add("v1. Лема не супадае з 0 формай: " + p(p));
        }
    }

    /**
     * Правяраем зворотнасьць
     */
    static void checkV4(Paradigm p) {
        char zv = p.getTag().charAt(3);
        char zvp;
        String lemma = StressUtils.unstress(p.getLemma());
        if (lemma.endsWith("ся") || lemma.endsWith("цца")) {
            zvp = 'R';
        } else if (lemma.endsWith("ць") || lemma.endsWith("чы") || lemma.endsWith("ці")) {
            zvp = 'N';
        } else {
            errors.add("v4. Незразумелая зваротнасьць па канчатку: " + p(p));
            return;
        }
        if (zv != 'X' && zv != zvp) {
            errors.add("v4. Зваротнасьць няправільна пазначана: " + p(p));
            return;
        }
    }

    /**
     * Правяраем спражэньне
     */
    static void checkV5(Paradigm p) {
        if (p.getLemma().equals("быць") || p.getLemma().equals("е´сці")) {
            return;
        }

        String tag;
        switch (p.getTag().charAt(2)) {
        case 'P':
            tag = "F3P";
            break;
        case 'M':
            tag = "R3P";
            break;
        default:
            errors.add("5. Невядомае спражэньне ў "+p(p));
            return;
        }

        Form r3p = getForm(p, tag);
        if (r3p == null) {
            errors.add("5. Няма " + tag + ": " + p(p));
            return;
        }
        char sp = p.getTag().charAt(4);
        char spp;
        String w = StressUtils.unstress(r3p.getValue());
        if (!w.isEmpty()) {
            if (w.endsWith("уць") || w.endsWith("юць") || w.endsWith("уцца") || w.endsWith("юцца")) {
                spp = '1';
            } else if (w.endsWith("аць") || w.endsWith("яць") || w.endsWith("ацца") || w.endsWith("яцца")) {
                spp = '2';
            } else {
                errors.add("5. Незразумелае спражэньне па канчатку: " + p(p) + ": " + r3p.getValue());
                return;
            }
            if (sp != 'X' && sp != spp) {
                errors.add("5. Спражэньне няправільна пазначана: " + p(p));
                return;
            }
        }
    }

    /**
     * 6. V?M??: павінны быць формы RG: -учы, -ючы, -учыся, -ючыся
     * 
     * 7. V?P??: PG толькі канчаткі: -аўшы, -яўшы, -аўшыся, -яўшыся
     */
    static void checkV67(Paradigm p) {
        char tr = p.getTag().charAt(2);
        switch (tr) {
        case 'M':
            Form rg = getForm(p, "RG");
            if (rg == null) {
                errors.add("6. Няма RG: " + p(p));
            } else {
                if (!rg.getValue().isEmpty()) {
                    String wk = StressUtils.unstress(rg.getValue());
                    if (!wk.endsWith("учы") && !wk.endsWith("ючы") && !wk.endsWith("учыся")
                            && !wk.endsWith("ючыся") && !wk.endsWith("ячы") && !wk.endsWith("ячыся")
                            && !wk.endsWith("ачы") && !wk.endsWith("ачыся")) {
                        errors.add("6. Няправільны канчатак RG: " + p(p) + ": " + wk);
                    }
                }
            }
            break;
        case 'P':
            Form pg = getForm(p, "PG");
            if (pg == null) {
                errors.add("6. Няма PG: " + p(p));
            } else {
                if (!pg.getValue().isEmpty()) {
                    String wk = StressUtils.unstress(pg.getValue());
                    if (!wk.endsWith("шы") && !wk.endsWith("шыся")) {
                        errors.add("6. Няправільны канчатак PG: " + p(p) + ": " + wk);
                    }
                }
            }
            break;
        default:
            errors.add("6/7. Непазначана трываньне: " + p(p));
            break;
        }
    }

    /**
     * праверыць усе формы - павінны быць аднолькавымі ва ўсіх дзеясловах для закончанага і незакончанага
     * трываньня
     */
    static void checkV8(Paradigm p) {
        char tr = p.getTag().charAt(2);
        String[] formTags;
        switch (tr) {
        case 'M':
            formTags = new String[] { "0", "R1S", "R2S", "R3S", "R1P", "R2P", "R3P", "PXSM", "PXSF", "PXSN",
                    "PXPX", "I2S", "I2P", "RG" };
            break;
        case 'P':
            formTags = new String[] { "0", "F1S", "F2S", "F3S", "F1P", "F2P", "F3P", "PXSM", "PXSF", "PXSN",
                    "PXPX", "I2S", "I2P", "PG" };
            break;
        default:
            return;
        }

        List<Form> standardForms = new ArrayList<>(p.getForm());
        for (int i = 0; i < standardForms.size(); i++) {
            if (standardForms.get(i).getType() == Type.NONSTANDARD) {
                standardForms.remove(i);
                i--;
            }
            // if ("potential".equals(standardForms.get(i).getType())) {
            // standardForms.remove(i);
            // i--;
            // }
        }

        if (standardForms.size() != formTags.length) {
            errors.add("8.1. Няправільныя формы ў " + p(p));
            return;
        }
        for (int i = 0; i < formTags.length; i++) {
            if (!standardForms.get(i).getTag().equals(formTags[i])) {
                errors.add("8.2. Няправільная форма ў " + p(p));
                return;
            }
        }
    }

    static Form getForm(Paradigm p, String formTag) {
        for (Form f : p.getForm()) {
            if (f.getTag().equals(formTag)) {
                return f;
            }
        }
        return null;
    }

    static String p(Paradigm p) {
        return p.getPdgId() + "/" + p.getTag() + "/" + p.getLemma();
    }
}
