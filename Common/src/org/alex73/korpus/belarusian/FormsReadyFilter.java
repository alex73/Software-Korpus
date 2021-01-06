package org.alex73.korpus.belarusian;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alex73.corpus.paradigm.Form;
import org.alex73.corpus.paradigm.FormType;
import org.alex73.corpus.paradigm.Paradigm;
import org.alex73.corpus.paradigm.Variant;
import org.alex73.korpus.utils.SetUtils;

/**
 * Фільтруе толькі тыя варыянты і формы, якія варта паказваць карыстальніку і
 * экспартаваць у праверку правапісу.
 */
public class FormsReadyFilter {
    public enum MODE {
        // spell checker - A2008 with official dictionaries
        SPELL,
        // for show - A2008 with any dictionaries
        SHOW
    };

    public static List<Form> getAcceptedForms(MODE mode, Paradigm p, Variant v) {
        if (v.getForm().isEmpty()) {
            return null;
        }
        String tag = SetUtils.tag(p, v);
        switch (mode) {
        case SPELL:
            if (tag.startsWith("K") || tag.startsWith("F") || v.getLemma().contains(" ")) {
                return null;
            }
            break;
        case SHOW:
            if (tag.startsWith("K")) {
                return null;
            }
            break;
        }
        boolean hasSlouniki = false;
        if (mode == MODE.SHOW) {
            if (v.getSlouniki()!=null && !v.getSlouniki().isEmpty()) {
                hasSlouniki = true; // правапіс - без піскунова, база - з піскуновым
            }
        }
        if (v.getForm().get(0).getSlouniki() != null) {
            for (String sl : v.getForm().get(0).getSlouniki().split(",")) {
                if (sl.isEmpty()) {
                    continue;
                }
                hasSlouniki = true;
                break;
            }
        }
        if (!tag.startsWith("NP") && !hasSlouniki) {
            return null;
        }
        Stream<Form> result;
        if (SetUtils.hasPravapis(v, "A2008")) {
            result = v.getForm().stream();
        } else if (v.getPravapis() == null) {
            result = v.getForm().stream().filter(f -> SetUtils.hasPravapis(f, "A2008"));
        } else {
            return null;
        }
        List<Form> r = result.filter(standardForms).filter(f -> !f.getValue().isEmpty()).collect(Collectors.toList());
        return r.isEmpty() ? null : r;
    }

    private static Predicate<Form> standardForms = (f) -> (f.getType() == null || f.getType() == FormType.NUMERAL);
}
