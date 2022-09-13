package org.alex73.korpus.compiler;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;

public class TextOrder implements Comparator<TextInfo> {
    static final List<String> subcorpuses = Arrays.asList("teksty", "kankardans", "sajty", "wiki", "pieraklady",
            "nierazabranaje", "telegram", "dyjalektny", "skaryna");
    static final Collator BE = Collator.getInstance(new Locale("be"));

    @Override
    public int compare(TextInfo o1, TextInfo o2) {
        if (o1 == o2) {
            return 0;
        }
        int c = Integer.compare(subcorpuses.indexOf(o1.subcorpus), subcorpuses.indexOf(o2.subcorpus));
        if (c == 0) {
            switch (o1.subcorpus) {
            case "teksty":
                c = teksty.compare(o1, o2);
                break;
            case "pieraklady":
                c = pieraklady.compare(o1, o2);
                break;
            case "wiki":
                c = wiki.compare(o1, o2);
                break;
            case "sajty":
                c = sajty.compare(o1, o2);
                break;
            case "nierazabranaje":
                c = nierazabranaje.compare(o1, o2);
                break;
            case "telegram":
                c = telegram.compare(o1, o2);
                break;
            case "kankardans":
                c = kankardans.compare(o1, o2);
                break;
            case "dyjalektny":
                c = dyjalektny.compare(o1, o2);
                break;
            case "skaryna":
                c = skaryna.compare(o1, o2);
                break;
            default:
                if (o1.subcorpus.startsWith("wiki")) {
                    c = wiki.compare(o1, o2);
                } else {
                    throw new RuntimeException("Unknown subcorpus: " + o1.subcorpus);
                }
            }
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

    Comparator<TextInfo> teksty = (o1, o2) -> {
        // першыя - найбольш раннія па даце стварэння альбо даце выдання. не пазначана
        // дата - на канец
        int c = Long.compare(earliestCreationPublication(o1, Long.MAX_VALUE),
                earliestCreationPublication(o2, Long.MAX_VALUE));
        if (c == 0) {
            c = compareAuthors(o1, o2);
        }
        if (c == 0) {
            c = BE.compare(o1.title, o2.title);
        }
        return c;
    };
    Comparator<TextInfo> pieraklady = (o1, o2) -> {
        // першыя - найбольш раннія па даце выдання. не пазначана дата - на канец
        int c = Long.compare(earliestPublication(o1, Long.MAX_VALUE), earliestPublication(o2, Long.MAX_VALUE));
        if (c == 0) {
            c = compareAuthors(o1, o2);
        }
        if (c == 0) {
            c = BE.compare(o1.title, o2.title);
        }
        return c;
    };
    Comparator<TextInfo> wiki = (o1, o2) -> {
        int c = o1.subcorpus.compareTo(o2.subcorpus);
        if (c == 0) {
            c = BE.compare(o1.title, o2.title);
        }
        return c;
    };
    Comparator<TextInfo> dyjalektny = (o1, o2) -> {
        int c = BE.compare(o1.details, o2.details);
        if (c == 0) {
            c = BE.compare(o1.source, o2.source);
        }
        return c;
    };
    Comparator<TextInfo> skaryna = (o1, o2) -> {
        int c = BE.compare(o1.title, o2.title);
        return c;
    };
    Comparator<TextInfo> kankardans = (o1, o2) -> {
        return Integer.compare(o1.textOrder, o2.textOrder);
    };
    Comparator<TextInfo> sajty = (o1, o2) -> {
        // першыя - найбольш познія. не пазначана дата - на канец
        int c = Long.compare(latestPublication(o2, Long.MAX_VALUE), latestPublication(o1, Long.MAX_VALUE));
        if (c == 0) {
            c = compare(o1.source, o2.source);
        }
        return c;
    };
    Comparator<TextInfo> nierazabranaje = (o1, o2) -> {
        return 0;
    };
    Comparator<TextInfo> telegram = (o1, o2) -> {
        // першыя - найбольш познія. не пазначана дата - на канец
        int c = Long.compare(latestPublication(o2, Long.MAX_VALUE), latestPublication(o1, Long.MAX_VALUE));
        if (c == 0) {
            c = compare(o1.source, o2.source);
        }
        return c;
    };

    static long earliestCreationPublication(TextInfo ti, long defaultValue) {
        if (ti.creationTimeEarliest() != null) {
            return ti.creationTimeEarliest();
        } else if (ti.publicationTimeEarliest() != null) {
            return ti.publicationTimeEarliest();
        } else {
            return defaultValue;
        }
    }

    static long earliestPublication(TextInfo ti, long defaultValue) {
        if (ti.publicationTimeEarliest() != null) {
            return ti.publicationTimeEarliest();
        } else {
            return defaultValue;
        }
    }

    static long latestPublication(TextInfo ti, long defaultValue) {
        if (ti.creationTimeLatest() != null) {
            return ti.creationTimeLatest();
        } else if (ti.publicationTimeLatest() != null) {
            return ti.publicationTimeLatest();
        } else {
            return defaultValue;
        }
    }

    static int compareAuthors(TextInfo o1, TextInfo o2) {
        int ac1 = o1.authors != null ? o1.authors.length : 0;
        int ac2 = o2.authors != null ? o2.authors.length : 0;
        int c = 0;
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
        return c;
    }

    static int compare(String s1, String s2) {
        if (s1 == null)
            s1 = "";
        if (s2 == null)
            s2 = "";
        return s1.compareTo(s2);
    }
}
