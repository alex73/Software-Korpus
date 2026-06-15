package org.alex73.korpus.base;

import org.alex73.grammardb.GrammarDB2;
import org.alex73.grammardb.structures.Form;
import org.alex73.grammardb.structures.Paradigm;
import org.alex73.grammardb.structures.Variant;

public class GrammarDBUtils {
    /**
     * Minimizes memory usage for strings in GrammarDB.
     */
    public static void minimizeMemory(GrammarDB2 db) {
        for (Paradigm p : db.getAllParadigms()) {
            p.setLemma(p.getLemma() == null ? null : p.getLemma().intern());
            p.setTag(p.getTag() == null ? null : p.getTag().intern());
            p.setTheme(p.getTheme() == null ? null : p.getTheme().intern());
            p.setMeaning(p.getMeaning() == null ? null : p.getMeaning().intern());
            for (Variant v : p.getVariant()) {
                v.setPravapis(v.getPravapis() == null ? null : v.getPravapis().intern());
                v.setSlouniki(v.getSlouniki() == null ? null : v.getSlouniki().intern());
                v.setLemma(v.getLemma() == null ? null : v.getLemma().intern());
                v.setTag(v.getTag() == null ? null : v.getTag().intern());
                v.setPrystauki(v.getPrystauki() == null ? null : v.getPrystauki().intern());
                v.setZmienyFanietyki(v.getZmienyFanietyki() == null ? null : v.getZmienyFanietyki().intern());
                for (Form f : v.getForm()) {
                    f.setValue(f.getValue().intern());
                    f.setGovern(f.getGovern() == null ? null : f.getGovern().intern());
                    f.setPravapis(f.getPravapis() == null ? null : f.getPravapis().intern());
                    f.setSlouniki(f.getSlouniki() == null ? null : f.getSlouniki().intern());
                    f.setTag(f.getTag() == null ? null : f.getTag().intern());
                }
            }
        }
    }
}
