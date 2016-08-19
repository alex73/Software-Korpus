package org.alex73.korpus.editor.grammar;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.base.BelarusianWordNormalizer;
import org.alex73.korpus.utils.StressUtils;

public class GrammarConstructor {
    private final EditorGrammar ed;

    public GrammarConstructor(EditorGrammar ed) {
        this.ed = ed;
    }

    public synchronized Paradigm getLooksLike(String word, String looksLike, boolean checkForms, String tagMask,
            StringBuilder out) {
        Paradigm ratedParadigm = null;
        Variant ratedVariant = null;
        String ratedForm = null;

        looksLike = looksLike.trim();
        if (looksLike.isEmpty()) {
            int rating = 1;
            String find = BelarusianWordNormalizer.normalize(word);
            for (Paradigm p : ed.getAllParadigms()) {
                if (!isTagLooksLikeMask(p.getTag(), tagMask)) {
                    continue;
                }
                int eq = compareEnds(find, BelarusianWordNormalizer.normalize(p.getLemma()));
                if (eq > rating) {
                    rating = eq;
                    ratedParadigm = p;
                    ratedVariant = p.getVariant().get(0);
                    ratedForm = p.getLemma();
                }
                if (checkForms) {
                    for (Variant v : p.getVariant()) {
                        for (Form f : v.getForm()) {
                            eq = compareEnds(find, BelarusianWordNormalizer.normalize(f.getValue()));
                            if (eq > rating) {
                                rating = eq;
                                ratedParadigm = p;
                                ratedVariant = v;
                                ratedForm = f.getValue();
                            }
                        }
                    }
                }
            }
        } else {
            String find = BelarusianWordNormalizer.normalize(looksLike);
            for (Paradigm p : ed.getAllParadigms()) {
                if (!isTagLooksLikeMask(p.getTag(), tagMask)) {
                    continue;
                }
                if (find.equals(BelarusianWordNormalizer.normalize(p.getLemma()))) {
                    ratedParadigm = p;
                    ratedVariant = p.getVariant().get(0);
                    ratedForm = p.getLemma();
                }
                if (checkForms) {
                    for (Variant v : p.getVariant()) {
                        for (Form f : v.getForm()) {
                            if (find.equals(BelarusianWordNormalizer.normalize(f.getValue()))) {
                                ratedParadigm = p;
                                ratedVariant = v;
                                ratedForm = f.getValue();
                            }
                        }
                    }
                }
            }
        }
        if (ratedParadigm == null) {
            return null;
        }
        out.append(ratedParadigm.getLemma() + "/" + ratedParadigm.getTag());
        return constructParadigm(word, ratedParadigm, ratedVariant, ratedForm);
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

    private static int compareEnds(String w1, String w2) {
        int eq = 0;
        for (int i1 = w1.length() - 1, i2 = w2.length() - 1; i1 >= 0 && i2 >= 0; i1--, i2--) {
            if (w1.charAt(i1) != w2.charAt(i2)) {
                break;
            }
            eq++;
        }
        return eq;
    }

    public Paradigm parseAndValidate(String pText) throws Exception {
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
     */
    Paradigm constructParadigm(String word, Paradigm p, Variant v, String ratedForm) {
        String unstressedWord = BelarusianWordNormalizer.normalize(word);
        String unstressedRatedForm = BelarusianWordNormalizer.normalize(ratedForm);
        int eq = compareEnds(unstressedWord, unstressedRatedForm);
        int ratedSkip = unstressedRatedForm.length() - eq;
        Paradigm result = new Paradigm();
        result.setTag(p.getTag());

        int stressInSource = StressUtils.getStressFromStart(word);

        String lemma = constructWord(unstressedWord, eq, BelarusianWordNormalizer.normalize(p.getLemma()), ratedSkip);
        int st = StressUtils.getUsuallyStressedSyll(lemma);
        if (st < 0) {
            st = stressInSource;
        }
        if (st >= 0) {
            lemma = StressUtils.setStressFromStart(lemma, st);
        } else {
            st = StressUtils.getStressFromEnd(p.getLemma());
            lemma = StressUtils.setStressFromEnd(lemma, st);
        }

        result.setLemma(lemma);
        Variant rv = new Variant();
        rv.setPravapis(v.getPravapis());
        for (Form f : v.getForm()) {
            Form rf = new Form();
            rf.setTag(f.getTag());
            String fword = constructWord(unstressedWord, eq, BelarusianWordNormalizer.normalize(f.getValue()),
                    ratedSkip);
            if (!fword.isEmpty()) {
                st = StressUtils.getUsuallyStressedSyll(fword);
                if (st < 0) {
                    st = stressInSource;
                }
                if (st >= 0) {
                    fword = StressUtils.setStressFromStart(fword, st);
                } else {
                    st = StressUtils.getStressFromEnd(f.getValue());
                    fword = StressUtils.setStressFromEnd(fword, st);
                }
            }
            rf.setValue(fword);
            rv.getForm().add(rf);
        }
        result.getVariant().add(rv);
        return result;
    }

    private static String constructWord(String originalWord, int eq, String form, int formSkip) {
        if (form.length() < formSkip) {
            return "????????????????";
        }
        String origBeg = originalWord.substring(0, originalWord.length() - eq);
        String formEnd = form.substring(formSkip);
        return origBeg + formEnd;
    }
}
