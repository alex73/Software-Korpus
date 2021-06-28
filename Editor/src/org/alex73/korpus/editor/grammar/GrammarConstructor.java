package org.alex73.korpus.editor.grammar;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

public class GrammarConstructor {
    public static final int MAX_RESULTS = 20;

    public final EditorGrammar ed;
    public List<List<PVW>> scores = new ArrayList<>();

    public interface Comparer {
        /**
         * @param target     - normalized word
         * @param comparable - normalized word
         */
        int getScore(String target, String comparable);
    }

    public GrammarConstructor(EditorGrammar ed) {
        this.ed = ed;
    }

    static Comparer eqEqual = new Comparer() {
        @Override
        public int getScore(String target, String comparable) {
            return target.equals(comparable) ? 1 : 0;
        }
    };
    static Comparer eqEnds = new Comparer() {
        @Override
        public int getScore(String target, String dbValue) {
            boolean removeDbStress = !StressUtils.hasStress(target);
            int eq = 0;
            for (int i1 = target.length() - 1, i2 = dbValue.length() - 1; i1 >= 0 && i2 >= 0; i1--, i2--) {
                char t1 = target.charAt(i1);
                char d1 = dbValue.charAt(i2);
                if (removeDbStress && d1 == StressUtils.STRESS_CHAR) {
                    i1++;
                    continue;
                }
                if (t1 != d1) {
                    break;
                }
                eq++;
            }
            return eq;
        }
    };

    public List<PVW> getLooksLike(String word, String filter, boolean preserveCase, String tagMask,
            Integer skipParadigmId) {
        long be = System.currentTimeMillis();
        final Comparer comparer;
        String target;
        boolean checkForms = true;
        Pattern reFilter;
        target = BelarusianWordNormalizer.lightNormalized(word);
        String strFilter = BelarusianWordNormalizer.lightNormalizedWithStars(filter);
        if (!strFilter.isEmpty() && strFilter.contains("*")) {
            reFilter = Pattern.compile(strFilter.replace("+", "\\+").replace("*", ".*"));
        } else {
            reFilter = null;
        }
        comparer = eqEnds;
        ed.getAllParadigms().parallelStream().forEach(p -> {
            if (skipParadigmId != null && skipParadigmId.intValue() == p.getPdgId()) {
                return;
            }
            for (Variant v : p.getVariant()) {
                if (!isTagLooksLikeMask(SetUtils.tag(p, v), tagMask)) {
                    return;
                }
                if (reFilter != null) {
                    if (!reFilter.matcher(v.getLemma()).matches()) {
                        continue;
                    }
                } else if (!strFilter.isEmpty()) {
                    if (!strFilter.equals(v.getLemma())) {
                        continue;
                    }
                }
                if (v.getForm().isEmpty()) {
                    continue; // неразгорнутыя
                }
                if (checkForms) {
                    for (Form f : v.getForm()) {
                        int score = comparer.getScore(target, f.getValue());
                        addToScores(score, p, v, f.getValue());
                    }
                } else {
                    int score = comparer.getScore(target, v.getLemma());
                    addToScores(score, p, v, v.getLemma());
                }
            }
        });

        if (scores.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> dump = new ArrayList<>();
        for (int i = 0, idx = scores.size() - 1; idx > 0 && i <= 4; idx--, i++) {
            if (!scores.get(idx).isEmpty()) {
                dump.add("=== " + target + " found with score=" + (idx + 1) + " ===");
                for (PVW s : scores.get(idx)) {
                    dump.add("  " + s.w + "   -> " + s.p.getTag() + "/" + s.v.getLemma());
                }
            }
        }
        // get best
        // PVW best = scores.get(scores.size() - 1).get(0);
//        out.append(String.format("%d%s/%s/%s (супадзенне па %d літарах)", best.p.getPdgId(), best.v.getId(), best.p.getTag(), best.p.getLemma(), scores.size()));
        long af = System.currentTimeMillis();
        System.out.println("Looks like exec time: " + (af - be) + "ms");

        // Paradigm result = constructParadigm(wordNormalized, best.p, best.v, best.w);
        List<PVW> result = new ArrayList<>();
        for (int p = scores.size() - 1; p >= 0; p--) {
            if (result.size() > 20) {
                break;
            }
            List<PVW> result1 = scores.get(p);
            Collections.sort(result1, (a, b) -> Integer.compare(a.p.getPdgId(), b.p.getPdgId()));
            result.addAll(result1);
        }
        return result;
    }

    public void addToScores(int score, Paradigm p, Variant v, String w) {
        if (score <= 0) {
            return;
        }
        PVW d = new PVW();
        d.p = p;
        d.v = v;
        d.w = w;
        synchronized (scores) {
            while (scores.size() < score) {
                scores.add(new ArrayList<>());
            }
            List<PVW> scored = scores.get(score - 1);
            if (scored.size() < MAX_RESULTS) {
                for (PVW ex : scored) {
                    if (ex.equalsPV(d)) {
                        return;
                    }
                }
                scored.add(d);
            }
        }
    }

    /**
     * Mask can be just start of tag, and can contains '?'.
     */
    private static boolean isTagLooksLikeMask(String tag, String mask) {
        if (mask.isEmpty()) {
            return true;
        }
        if (mask.length() > tag.length()) {
            return false;
        }
        for (int i = 0; i < mask.length(); i++) {
            char cT = tag.charAt(i);
            char cM = mask.charAt(i);
            if (cT != cM && cM != '?') {
                return false;
            }
        }
        return true;
    }

    public static Paradigm parseAndValidate(String pText) throws Exception {
        Validator validator = EditorGrammar.schema.newValidator();

        Source source = new StreamSource(new StringReader(pText));
        validator.validate(source);

        Unmarshaller unm = EditorGrammar.CONTEXT.createUnmarshaller();
        Paradigm p = (Paradigm) unm.unmarshal(new StringReader(pText));
        // check stress
        for (Variant v : p.getVariant()) {
            for (Form f : v.getForm()) {
                StressUtils.checkStress(f.getValue());
            }
        }
        return p;
    }

    public String toText(Paradigm p) throws Exception {
        Marshaller m = EditorGrammar.CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        m.marshal(p, writer);
        return writer.toString();
    }

    /**
     * Construct new paradigm based on specified paradigm.
     * 
     * Stress syll based on rules:
     * 
     * 1. usually stressed chars
     * 
     * 2. otherwise, the same syll from word start if 'word' has stress
     * 
     * 3. otherwise, the same syll from word end if paradigm has stress
     * 
     * @param word      - normalized word
     * @param ratedForm - normalized word (from grammar db)
     */
    public Paradigm constructParadigm(String word, Paradigm p, Variant v, String ratedForm) {
        int eq = eqEnds.getScore(word, ratedForm);

        Paradigm result = new Paradigm();
        result.setTag(p.getTag());

        String lemma = constructWord(word, ratedForm, eq, v.getLemma());

        result.setLemma(lemma);
        Variant rv = new Variant();
        rv.setId("a");
        rv.setLemma(lemma);
        rv.setPravapis(v.getPravapis());
        for (Form f : v.getForm()) {
            Form rf = new Form();
            rf.setTag(f.getTag());
            String fword = constructWord(word, ratedForm, eq, f.getValue());
            rf.setValue(fword);
            rv.getForm().add(rf);
        }
        result.getVariant().add(rv);
        return result;
    }

    static String constructWord(String originalWord, String ratedForm, int eq, String form) {
        if (form.length() < ratedForm.length() - eq) {
            return "????????????????";
        }
        String normalizedForm, normalizedRatedForm;
        if (!StressUtils.hasStress(originalWord)) {
            normalizedForm = StressUtils.unstress(form);
            normalizedRatedForm = StressUtils.unstress(ratedForm);
        } else {
            normalizedForm = form;
            normalizedRatedForm = ratedForm;
        }
        String origBeg = originalWord.substring(0, originalWord.length() - eq);
        String formEnd = normalizedForm.substring(normalizedRatedForm.length() - eq);
        String result = origBeg + formEnd;

        if (!StressUtils.hasStress(result)) {
            int st = StressUtils.getUsuallyStressedSyll(result, -1);
            if (st < 0) {
                st = StressUtils.getStressFromStart(originalWord);
            }
            if (st >= 0) {
                result = StressUtils.setStressFromStart(result, st);
            } else {
                st = StressUtils.getStressFromEnd(form);
                result = StressUtils.setStressFromEnd(result, st);
            }
        }
        return result;
    }

    public static class PVW {
        public Paradigm p;
        public Variant v;
        public String w;

        public boolean equalsPV(PVW oth) {
            return p == oth.p && v == oth.v;
        }
    }
}
