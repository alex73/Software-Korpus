package org.alex73.korpus.future;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.grammardb.StressUtils;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;
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

@Path("/conv")
public class Kanvertary {
    private final static Logger LOGGER = Logger.getLogger(Kanvertary.class.getName());

    private final static ILanguage.INormalizer NORMALIZER = LanguageFactory.get("bel").getNormalizer();

    @POST
    @Path("fanetyka")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces("text/html; charset=UTF-8")
    public String fanetykaLog(String text) throws Exception {
        try {
            if (text.isBlank()) {
                throw new Exception("Пусты тэкст");
            }
            FanetykaText f = new FanetykaText(ApplicationOther.instance.grFinder,
                    text.replace('+', BelarusianWordNormalizer.pravilny_nacisk).replaceAll("[-‒‒–]", "-"));
            String o = "<div id='log' style='display: none'>";
            for (String log : f.why) {
                o += "<hr/>\n" + log;
            }
            o+="</div>";

            return "<div>Вынікі канвертавання (IPA):</div><div style='font-size: 150%'>" + f.ipa.replace("\n", "<br/>")
                    + "</div><br/><div>Школьная транскрыпцыя:</div><div style='font-size: 150%'>" + f.skola.replace("\n", "<br/>") + "</div>" + o;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Памылка пры канвертаванні '" + text + "'", ex);
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

    @POST
    @Path("synth")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response synth(String text) throws Exception {
        if (text.isBlank()) {
            throw new Exception("Пусты тэкст");
        }
        if (text.trim().startsWith(ApplicationOther.instance.synthBookPrefix)) {
            String r = processBook(text.trim().substring(ApplicationOther.instance.synthBookPrefix.length()).trim());
            return Response.status(Response.Status.OK).type(MediaType.TEXT_PLAIN).entity(r).build();
        }
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }

        try {
            FanetykaText f = new FanetykaText(ApplicationOther.instance.grFinder,
                    text.replace('+', BelarusianWordNormalizer.pravilny_nacisk).replaceAll("[-‒‒–]", "-"));

            synchronized (ApplicationOther.instance.synthUrl) { // prevent server high load
                String ipa = f.ipa.replace("w", "u̯").replace("w", "u̯").replace("ɱ", "m").replace("β", "v").replace("ʋ", "v")
                        .replace("\u031E", "").replace("\u032A", "").replace("\u0331", "").replace("\u035F", "");
                URL url = new URI(ApplicationOther.instance.synthUrl + "?text=" + URLEncoder.encode(ipa, StandardCharsets.UTF_8)).toURL();
                try (InputStream in = url.openStream()) {
                    byte[] r = in.readAllBytes();
                    return Response.status(Response.Status.OK).type("audio/wav").entity(r).build();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    String processBook(String text) throws Exception {
        String name = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        java.nio.file.Path dir = Paths.get(ApplicationOther.instance.synthBookDir).resolve(name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("status.txt"), "Канвертацыя фанетыкі...");
        new Thread() {
            public void run() {
                try {
                    FanetykaText f = new FanetykaText(ApplicationOther.instance.grFinder,
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
        return "Кніга захаваная <a href='" + ApplicationOther.instance.synthBookUrl + name + "/'>тут</a> для апрацоўкі";
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
