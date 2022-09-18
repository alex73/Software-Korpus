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
import org.alex73.korpus.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.server.KorpusApplication;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;
import org.alex73.korpus.utils.StressUtils;

@Path("/naciski")
public class Naciski {

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("text/plain; charset=UTF-8")
    public String search(String text) throws Exception {
        Splitter3 splitter = new Splitter3(false, new IProcess() {
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
                String word = StressUtils.unstress(w.lightNormalized);
                out.append(wordAccent(word));
            } else if (el instanceof SentenceSeparatorItem) {
            } else {
                out.append(el.getText());
            }
        }

        return out.toString();
    }

    String wordAccent(String word) {
        Set<Integer> stresses = new TreeSet<>();
        for (Paradigm p : KorpusApplication.instance.grFinder.getParadigms(word)) {
            for (Variant v : p.getVariant()) {
                for (Form f : v.getForm()) {
                    if (f.getValue().isEmpty()) {
                        continue;
                    }
                    if (BelarusianWordNormalizer.equals(f.getValue(), word)) {
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
