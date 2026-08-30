package org.alex73.korpus.future;

import org.alex73.fanetyka.impl.FanetykaText;
import org.alex73.grammardb.GrammarFinder;
import org.alex73.korpus.languages.ILanguage;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.languages.belarusian.BelarusianWordNormalizer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Kanvertary {
    private final static Logger LOGGER = Logger.getLogger(Kanvertary.class.getName());

    private final static ILanguage.INormalizer NORMALIZER = LanguageFactory.get("bel").getNormalizer();
    private final GrammarFinder grFinder;

    public Kanvertary(GrammarFinder grFinder) {
        this.grFinder = grFinder;
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
}
