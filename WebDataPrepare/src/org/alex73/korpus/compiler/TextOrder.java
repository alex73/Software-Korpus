package org.alex73.korpus.compiler;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.utils.KorpusDateTime;

public class TextOrder implements Comparator<TextInfo> {
    static final List<String> subcorpuses = Arrays.asList("teksty", "sajty", "wiki", "pieraklady", "nierazabranaje",
            "telegram");
    static final Collator BE = Collator.getInstance(new Locale("be"));

    @Override
    public int compare(TextInfo o1, TextInfo o2) {
        if (o1 == o2) {
            return 0;
        }
        int c = Integer.compare(subcorpuses.indexOf(o1.subcorpus), subcorpuses.indexOf(o2.subcorpus));
        if (c == 0 && o1.subcorpus.equals("wiki")) {
            c = Integer.compare(wikiOrder(o1), wikiOrder(o2));
        }
        if (c == 0) {
            switch (o1.subcorpus) {
            case "teksty":
            case "pieraklady":
                // першыя - найбольш раннія. не пазначана дата - на канец
                c = Long.compare(earliest(o1, Long.MAX_VALUE), earliest(o2, Long.MAX_VALUE));
                break;
            default:
                // першыя - найбольш апошнія. не пазначана дата - на канец
                c = Long.compare(latest(o2, Long.MIN_VALUE), latest(o1, Long.MIN_VALUE));
                break;
            }
        }
        if (c == 0) {
            int ac1 = o1.authors != null ? o1.authors.length : 0;
            int ac2 = o2.authors != null ? o2.authors.length : 0;
            for (int i = 0; i < Math.max(ac1, ac2); i++) {
                String a1, a2;
                try {
                    a1 = o1.authors[i];
                } catch (Exception ex) {
                    a1 = "";
                }
                try {
                    a2 = o2.authors[i];
                } catch (Exception ex) {
                    a2 = "";
                }
                c = BE.compare(a1, a2);
                if (c != 0) {
                    break;
                }
            }
        }
        if (c == 0) {
            c = BE.compare(o1.title, o2.title);
        }
        if (c == 0) {
            c = compare(o1.url, o2.url);
        }
        if (c == 0) {
            c = compare(o1.sourceFilePath, o2.sourceFilePath);
        }
        if (c == 0) {
            throw new RuntimeException(
                    "Нявызначаны парадак тэкстаў для " + o1.sourceFilePath + " і " + o2.sourceFilePath);
        }
        return c;
    }

    static long earliest(TextInfo ti, long defaultValue) {
        if (ti.creationTime != null) {
            return new KorpusDateTime(ti.creationTime).earliest();
        } else if (ti.publicationTime != null) {
            return new KorpusDateTime(ti.publicationTime).earliest();
        } else {
            return defaultValue;
        }
    }

    static long latest(TextInfo ti, long defaultValue) {
        if (ti.creationTime != null) {
            return new KorpusDateTime(ti.creationTime).latest();
        } else if (ti.publicationTime != null) {
            return new KorpusDateTime(ti.publicationTime).latest();
        } else {
            return defaultValue;
        }
    }

    static int compare(String s1, String s2) {
        if (s1 == null)
            s1 = "";
        if (s2 == null)
            s2 = "";
        return s1.compareTo(s2);
    }

    static int wikiOrder(TextInfo ti) {
        if (ti.url.startsWith("https://be-tarask.")) {
            return 1;
        } else if (ti.url.startsWith("https://be.")) {
            return 0;
        } else {
            throw new RuntimeException("Невядомая спасылка на wiki: " + ti.url);
        }
    }

}
