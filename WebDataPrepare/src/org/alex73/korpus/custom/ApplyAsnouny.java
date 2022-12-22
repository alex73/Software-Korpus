package org.alex73.korpus.custom;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.ITextsPreprocessor;
import org.alex73.korpus.compiler.MessageParsedText;

public class ApplyAsnouny implements ITextsPreprocessor {
    @Override
    public void preprocess(MessageParsedText text) {
        ITextsPreprocessor.shuffle(text.paragraphs);

        if ("teksty".equals(text.textInfo.subcorpus) && text.textInfo.subtexts[0].langOrig != null) {
            // падкорпус перакладаў
            text.textInfo.subcorpus = "pieraklady";
        }
    }

    @Override
    public Comparator<TextInfo> getTextsComparator() {
        return (o1, o2) -> {
            if (o1.subtexts.length != 1) {
                throw new RuntimeException("Не адзін варыянт тэкста " + o1.sourceFilePath);
            }
            if (o2.subtexts.length != 1) {
                throw new RuntimeException("Не адзін варыянт тэкста " + o2.sourceFilePath);
            }
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
                throw new RuntimeException("Нявызначаны парадак тэкстаў для " + o1.sourceFilePath + " і " + o2.sourceFilePath);
            }
            return c;
        };
    }

    static final List<String> subcorpuses = Arrays.asList("skaryna", "kankardans", "teksty", "pieraklady", "sajty", "wiki", "nierazabranaje", "dyjalektny");
    static final Collator BE = Collator.getInstance(new Locale("be"));

    Comparator<TextInfo> teksty = (o1, o2) -> {
        // першыя - найбольш раннія па даце стварэння альбо даце выдання. не пазначана
        // дата - на канец
        int c = Long.compare(earliestCreationPublication(o1, Long.MAX_VALUE), earliestCreationPublication(o2, Long.MAX_VALUE));
        if (c == 0) {
            c = compareAuthors(o1, o2);
        }
        if (c == 0) {
            c = BE.compare(o1.subtexts[0].title, o2.subtexts[0].title);
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
            c = BE.compare(o1.subtexts[0].title, o2.subtexts[0].title);
        }
        return c;
    };
    Comparator<TextInfo> wiki = (o1, o2) -> {
        int c = o1.subcorpus.compareTo(o2.subcorpus);
        if (c == 0) {
            c = BE.compare(o1.subtexts[0].title, o2.subtexts[0].title);
        }
        return c;
    };
    Comparator<TextInfo> dyjalektny = (o1, o2) -> {
        int c = BE.compare(o1.subtexts[0].details, o2.subtexts[0].details);
        if (c == 0) {
            c = BE.compare(o1.subtexts[0].source, o2.subtexts[0].source);
        }
        return c;
    };
    Comparator<TextInfo> skaryna = (o1, o2) -> {
        int c = BE.compare(o1.subtexts[0].title, o2.subtexts[0].title);
        return c;
    };
    Comparator<TextInfo> kankardans = (o1, o2) -> {
        return Integer.compare(o1.textOrder, o2.textOrder);
    };
    Comparator<TextInfo> sajty = (o1, o2) -> {
        // першыя - найбольш познія. не пазначана дата - на канец
        int c = Long.compare(latestPublication(o2, Long.MAX_VALUE), latestPublication(o1, Long.MAX_VALUE));
        if (c == 0) {
            c = compare(o1.subtexts[0].source, o2.subtexts[0].source);
        }
        return c;
    };
    Comparator<TextInfo> nierazabranaje = (o1, o2) -> {
        return 0;
    };

    static long earliestCreationPublication(TextInfo ti, long defaultValue) {
        if (ti.subtexts[0].creationTimeEarliest() != null) {
            return ti.subtexts[0].creationTimeEarliest();
        } else if (ti.subtexts[0].publicationTimeEarliest() != null) {
            return ti.subtexts[0].publicationTimeEarliest();
        } else {
            return defaultValue;
        }
    }

    static long earliestPublication(TextInfo ti, long defaultValue) {
        if (ti.subtexts[0].publicationTimeEarliest() != null) {
            return ti.subtexts[0].publicationTimeEarliest();
        } else {
            return defaultValue;
        }
    }

    static long latestPublication(TextInfo ti, long defaultValue) {
        if (ti.subtexts[0].creationTimeLatest() != null) {
            return ti.subtexts[0].creationTimeLatest();
        } else if (ti.subtexts[0].publicationTimeLatest() != null) {
            return ti.subtexts[0].publicationTimeLatest();
        } else {
            return defaultValue;
        }
    }

    static int compareAuthors(TextInfo o1, TextInfo o2) {
        int ac1 = o1.subtexts[0].authors != null ? o1.subtexts[0].authors.length : 0;
        int ac2 = o2.subtexts[0].authors != null ? o2.subtexts[0].authors.length : 0;
        int c = 0;
        for (int i = 0; i < Math.max(ac1, ac2); i++) {
            String a1, a2;
            try {
                a1 = o1.subtexts[0].authors[i];
            } catch (Exception ex) {
                a1 = "";
            }
            try {
                a2 = o2.subtexts[0].authors[i];
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
