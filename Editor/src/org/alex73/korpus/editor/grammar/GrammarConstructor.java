package org.alex73.korpus.editor.grammar;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.utils.SetUtils;
import org.alex73.korpus.utils.StressUtils;

public class GrammarConstructor {
    public final EditorGrammar ed;
    public List<List<PVW>> scores = new ArrayList<>();

    public interface Comparer {
        /**
         * @param target
         *            - normalized word
         * @param comparable
         *            - normalized word
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
        public int getScore(String target, String comparable) {
            if (!StressUtils.hasStress(target)) {
                comparable = StressUtils.unstress(comparable);
            }
            int eq = 0;
            for (int i1 = target.length() - 1, i2 = comparable.length() - 1; i1 >= 0 && i2 >= 0; i1--, i2--) {
                if (target.charAt(i1) != comparable.charAt(i2)) {
                    break;
                }
                eq++;
            }
            return eq;
        }
    };

    public Paradigm getLooksLike(String word, String looksLike, boolean preserveCase, boolean checkForms, String tagMask, StringBuilder out,
            Integer skipParadigmId) {
        final Comparer comparer;
        String target;
        String wordNormalized = preserveCase ? BelarusianWordNormalizer.normalizePreserveCase(word)
                : BelarusianWordNormalizer.normalize(word);
        if (looksLike.isEmpty()) {
            target = wordNormalized;
            comparer = eqEnds;
        } else {
            target = BelarusianWordNormalizer.normalize(looksLike);
            comparer = eqEqual;
        }
        ed.getAllParadigms().parallelStream().forEach(p -> {
            if (skipParadigmId != null && skipParadigmId.intValue() == p.getPdgId()) {
                return;
            }
            for (Variant v : p.getVariant()) {
                if (!isTagLooksLikeMask(SetUtils.tag(p, v), tagMask)) {
                    return;
                }
                if (v.getForm().isEmpty()) {
                    continue;
                }
                if (checkForms) {
                    for (Form f : v.getForm()) {
                        int score = comparer.getScore(target, BelarusianWordNormalizer.normalize(f.getValue()));
                        addToScores(score, p, v, f.getValue());
                    }
                } else {
                    int score = comparer.getScore(target, BelarusianWordNormalizer.normalize(v.getLemma()));
                    addToScores(score, p, v, v.getLemma());
                }
            }
        });

        if (scores.isEmpty()) {
            return null;
        }
        List<String> dump=new ArrayList<>();
        for (int i = 0, idx = scores.size() - 1; idx > 0 && i <= 4; idx--, i++) {
            if (!scores.get(idx).isEmpty()) {
                dump.add("=== " + target + " found with score=" + (idx + 1) + " ===");
                for (PVW s : scores.get(idx)) {
                    dump.add("  " + s.w + "   -> " + s.p.getTag() + "/" + s.v.getLemma());
                }
            }
        }
        try {
        Files.write(Paths.get("dump.txt"), dump, StandardCharsets.UTF_8);
        } catch(Exception ex) {}
        // get best
        PVW best = scores.get(scores.size() - 1).get(0);
        out.append(best.p.getLemma() + "/" + best.p.getTag());
        return constructParadigm(wordNormalized, best.p, best.v, best.w);
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
            if (scores.get(score - 1).size() < 10) {
                scores.get(score - 1).add(d);
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
     * @param word
     *            - normalized word
     * @param ratedForm
     *            - normalized word (from grammar db)
     */
    Paradigm constructParadigm(String word, Paradigm p, Variant v, String ratedForm) {
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

    static class PVW {
        Paradigm p;
        Variant v;
        String w;
    }
}
