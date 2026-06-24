package org.alex73.korpus.future;

import io.javalin.http.Context;
import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianWordNormalizer;
import org.alex73.korpus.text.parser.IProcess;
import org.alex73.korpus.text.parser.Splitter3;
import org.alex73.korpus.text.structure.files.ITextLineElement;
import org.alex73.korpus.text.structure.files.SentenceSeparatorItem;
import org.alex73.korpus.text.structure.files.TextLine;
import org.alex73.korpus.text.structure.files.WordItem;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Kanvertary {
    private final static Logger LOGGER = Logger.getLogger(Kanvertary.class.getName());

    private final static ILanguage.INormalizer NORMALIZER = LanguageFactory.get("bel").getNormalizer();
    private final GrammarFinder grFinder;
    private final String synthBookDir;
    private final String synthBookUrl;
    private final String synthUrl;

    public Kanvertary(GrammarFinder grFinder, String synthBookDir, String synthBookUrl, String synthUrl) {
        this.grFinder = grFinder;
        this.synthBookDir = synthBookDir;
        this.synthBookUrl = synthBookUrl;
        this.synthUrl = synthUrl;
    }

    public String fanetykaLog(String text) throws Exception {
        try {
            if (text.isBlank()) {
                throw new Exception("Пусты тэкст");
            }
            FanetykaText f = new FanetykaText(grFinder,
                    text.replace('+', BelarusianWordNormalizer.pravilny_nacisk).replaceAll("[-‒‒–]", "-"));
            String o = "<div id='log' style='display: none'>";
            for (String log : f.why) {
                o += "<hr/>\n" + log;
            }
            o += "</div>";

            return "<div>Вынікі канвертавання (IPA):</div><div style='font-size: 150%'>" + f.ipa.replace("\n", "<br/>")
                    + "</div><br/><div>Школьная транскрыпцыя:</div><div style='font-size: 150%'>" + f.skola.replace("\n", "<br/>") + "</div>" + o;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Памылка пры канвертаванні '" + text + "'", ex);
            return "Памылка: " + ex.getMessage();
        }
    }

    public String naciski(String text) throws Exception {
        Splitter3 splitter = new Splitter3(LanguageFactory.get("bel").getNormalizer(), false, new IProcess() {
            @Override
            public synchronized void showStatus(String status) {
            }

            @Override
            public synchronized void reportError(String place, String error, Throwable ex) {
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

    public void synth(String text, Context ctx) throws Exception {
        try {
            FanetykaText f = new FanetykaText(grFinder,
                    text.replace('+', BelarusianWordNormalizer.pravilny_nacisk).replaceAll("[-‒‒–]", "-"));

            synchronized (synthBookUrl) { // prevent server high load
                String ipa = f.ipa.replace("w", "u̯").replace("w", "u̯").replace("ɱ", "m").replace("β", "v").replace("ʋ", "v")
                        .replace("\u031E", "").replace("\u032A", "").replace("\u0331", "").replace("\u035F", "");
                URL url = new URI(synthUrl + "?text=" + URLEncoder.encode(ipa, StandardCharsets.UTF_8)).toURL();
                try (InputStream in = url.openStream()) {
                    byte[] r = in.readAllBytes();
                    ctx.contentType("audio/wav").result(r);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ctx.status(500).result(ex.getMessage());
        }
    }

    public String processBook(String text) throws Exception {
        String name = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path dir = Paths.get(synthBookDir).resolve(name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("status.txt"), "Канвертацыя фанетыкі...");
        new Thread() {
            public void run() {
                try {
                    FanetykaText f = new FanetykaText(grFinder,
                            text.replace('+', BelarusianWordNormalizer.pravilny_nacisk).replaceAll("[-‒‒–]", "-"));
                    Files.writeString(dir.resolve("in.txt"), f.ipa);
                    Files.writeString(dir.resolve("status.txt"), "Чаканне пачатку агучкі...");
                } catch (Exception ex) {
                    try {
                        Files.writeString(dir.resolve("status.txt"), "Памылка канвертацыі фанетыкі: " + ex.getMessage());
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
        return "Кніга захаваная <a href='" + synthBookUrl + name + "/'>тут</a> для апрацоўкі";
    }

    String wordAccent(String word) {
        if (StressUtils.syllCount(word) < 2) {
            return word;
        }
        String wNormalized = NORMALIZER.lightNormalized(word, ILanguage.INormalizer.PRESERVE_NONE);
        Set<Integer> stresses = new TreeSet<>();
        for (Paradigm p : grFinder.getParadigms(word)) {
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
