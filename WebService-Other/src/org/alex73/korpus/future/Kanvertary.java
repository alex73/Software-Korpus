package org.alex73.korpus.future;

import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.server.ApplicationOther;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;
import org.alex73.korpus.utils.StressUtils;

@Path("/conv")
public class Kanvertary {
    private final static ILanguage.INormalizer NORMALIZER = LanguageFactory.get("bel").getNormalizer();

    @POST
    @Path("fanetyka")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("text/html; charset=UTF-8")
    public String fanetyka(String text) throws Exception {
        try {
            if (text.isBlank()) {
                throw new Exception("Пусты тэкст");
            }

            FanetykaText f = new FanetykaText(ApplicationOther.instance.grFinder, text.replace('+', BelarusianWordNormalizer.pravilny_nacisk).replaceAll("[-‒‒–]", "-"));

            return "<div>Вынікі канвертавання (IPA):</div><div style='font-size: 150%'>" + f.ipa.replace("\n", "<br/>")
                    + "</div><br/><div>Школьная транскрыпцыя:</div><div style='font-size: 150%'>" + f.skola.replace("\n", "<br/>") + "</div>";
        } catch (Exception ex) {
            return "Памылка: " + ex.getMessage();
        }
    }

    @POST
    @Path("naciski")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("text/plain; charset=UTF-8")
    public String naciski(String text) throws Exception {
        Splitter3 splitter = new Splitter3(LanguageFactory.get("bel").getNormalizer(), false, new IProcess() {
            @Override
            public synchronized void showStatus(String status) {
            }

            @Override
            public synchronized void reportError(String error, Throwable ex) {
                throw new RuntimeException(ex);
            }
        });
        TextLine line = splitter.parse(text);
        StringBuilder out = new StringBuilder();
        for (ITextLineElement el : line) {
            if (el instanceof WordItem w) {
                String word = StressUtils.unstress(w.word);
                out.append(wordAccent(word));
            } else if (el instanceof SentenceSeparatorItem) {
            } else {
                out.append(el.getText());
            }
        }

        return out.toString();
    }

    String wordAccent(String word) {
        if (StressUtils.syllCount(word) < 2) {
            return word;
        }
        String wNormalized = NORMALIZER.lightNormalized(word, ILanguage.INormalizer.PRESERVE_NONE);
        Set<Integer> stresses = new TreeSet<>();
        for (Paradigm p : ApplicationOther.instance.grFinder.getParadigms(word)) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (f.getValue().isEmpty()) {
                        continue;
                    }
                    if (NORMALIZER.lightNormalized(f.getValue(), ILanguage.INormalizer.PRESERVE_NONE).equals(wNormalized)) {
                        stresses.addAll(StressUtils.getAllStressesFromEnd(f.getValue()));
                    }
                }
            }
        }
        for (int pos : stresses) {
            word = StressUtils.setStressFromEnd(word, pos);
        }
        return word;
    }
}
