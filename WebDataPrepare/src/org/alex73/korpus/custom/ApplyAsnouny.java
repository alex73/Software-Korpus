package org.alex73.korpus.custom;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.ITextsPreprocessor;
import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.compiler.parsers.AuthorsUtil;

public class ApplyAsnouny implements ITextsPreprocessor {
    @Override
    public void preprocess(MessageParsedText text) {
        if (text.paragraphs != null) {
            ITextsPreprocessor.shuffle(text.paragraphs);
        }
        for (TextInfo.Subtext st : text.textInfo.subtexts) {
            if (st.lang == null) {
                st.lang = "bel";
            }
        }

        if ("teksty".equals(text.textInfo.subcorpus) && text.textInfo.subtexts[0].langOrig != null) {
            // падкорпус перакладаў
            text.textInfo.subcorpus = SUBCORPUSES.pieraklady.name();
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
            SUBCORPUSES s1 = SUBCORPUSES.valueOf(o1.subcorpus);
            SUBCORPUSES s2 = SUBCORPUSES.valueOf(o2.subcorpus);
            int c = s1.ordinal() - s2.ordinal();
            if (c == 0) {
                switch (s1) {
                case teksty:
                    c = teksty.compare(o1, o2);
                    break;
                case pieraklady:
                    c = pieraklady.compare(o1, o2);
                    break;
                case wiki:
                    c = wiki.compare(o1, o2);
                    break;
                case sajty:
                    c = sajty.compare(o1, o2);
                    break;
                case nierazabranaje:
                    c = nierazabranaje.compare(o1, o2);
                    break;
                case kankardans:
                    c = kankardans.compare(o1, o2);
                    break;
                case dyjalektny:
                    c = dyjalektny.compare(o1, o2);
                    break;
                case skaryna:
                    c = skaryna.compare(o1, o2);
                    break;
                default:
                    throw new RuntimeException("Unknown subcorpus: " + o1.subcorpus);
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

    @Override
    public void constructTextPassport(TextInfo textInfo, TextInfo.Subtext subText) {
        StringBuilder s = new StringBuilder();
        addHeader(s, "Падкорпус", "{{subcorpus:" + textInfo.subcorpus + "}}");

        SUBCORPUSES sub = SUBCORPUSES.valueOf(textInfo.subcorpus);
        switch (sub) {
        case teksty:
        case pieraklady: {
            subText.label = subText.authors != null ? String.join(",", subText.authors) : "———";
            String textAuthors = parseThenJoinAuthors(subText.headers.get("Authors"));
            subText.title = (textAuthors == null ? "" : (textAuthors + ".")) + subText.headers.get("Title") + "{{page}}";
            addHeader(s, "URL", url(subText.headers.get("URL")));
            addHeader(s, "Аўтары", textAuthors);
            addHeader(s, "Перакладчык", parseThenJoinAuthors(subText.headers.get("Translation")));
            addHeader(s, "Пераклад з", subText.headers.get("LangOrig"));
            addHeader(s, "Назва", subText.headers.get("Title") + "{{page}}");
            addHeader(s, "Стыль/жанр", subText.headers.get("StyleGenre"));
            addHeader(s, "Выданне", subText.headers.get("Edition"));
            addHeader(s, "Файл", subText.headers.get("File"));
            addHeader(s, "Час стварэння", subText.headers.get("CreationYear"));
            addHeader(s, "Час публікацыі", subText.headers.get("PublicationYear"));
            break;
        }
        case wiki:
            subText.label = subText.source;
            subText.title = subText.headers.get("Title");
            addHeader(s, "URL", url(subText.headers.get("URL")));
            addHeader(s, "Крыніца", subText.source);
            addHeader(s, "Назва", subText.headers.get("Title"));
            break;
        case skaryna:
            subText.label = subText.headers.get("Title");
            subText.title = subText.headers.get("Title");
            addHeader(s, "Назва", subText.headers.get("Title"));
            break;
        case dyjalektny:
            subText.label = subText.headers.get("Раён");
            subText.title = subText.headers.get("Раён");
            addHeader(s, "Вобласць", subText.headers.get("Вобласць"));
            addHeader(s, "Раён", subText.headers.get("Раён"));
            addHeader(s, "Месца", subText.headers.get("Месца"));
            addHeader(s, "Дыялект", subText.headers.get("Дыялект, група гаворак"));
            addHeader(s, "Збіральнік", subText.headers.get("Збіральнік"));
            addHeader(s, "Год запісу", subText.headers.get("Год запісу"));
            addHeader(s, "Тэкст расшыфраваў", subText.headers.get("Тэкст расшыфраваў"));
            addHeader(s, "Год нараджэння інфарманта", subText.headers.get("Інфармант-год нараджэння"));
            addHeader(s, "Месца нараджэння інфарманта", subText.headers.get("Інфармант-месца нараджэння"));
            addHeader(s, "Дзе жыў інфармант", subText.headers.get("Інфармант-дзе жыў"));
            addHeader(s, "Пол інфарманта", subText.headers.get("Інфармант-пол"));
            addHeader(s, "Нацыянальнасць інфарманта", subText.headers.get("Інфармант-нацыянальнасць"));
            addHeader(s, "Веравызнанне інфарманта", subText.headers.get("Інфармант-веравызнанне"));
            addHeader(s, "Адукацыя інфарманта", subText.headers.get("Інфармант-адукацыя"));
            addHeader(s, "Паходжанне бацькоў інфарманта", subText.headers.get("Інфармант-паходжанне бацькоў"));
            addHeader(s, "Тып тэксту інфарманта", subText.headers.get("Тып тэксту"));
            addHeader(s, "Крыніца", subText.headers.get("Крыніца"));
            break;
        case sajty:
            subText.label = subText.source;
            subText.title = subText.headers.get("Title");
            addHeader(s, "URL", url(subText.headers.get("URL")));
            addHeader(s, "Назва", subText.headers.get("Title"));
            addHeader(s, "Крыніца", subText.source);
            addHeader(s, "Час публікацыі", subText.headers.get("PublicationYear"));
            break;
        case nierazabranaje:
            subText.label = subText.source;
            subText.title = subText.headers.get("Title");
            addHeader(s, "URL", url(subText.headers.get("URL")));
            addHeader(s, "Назва", subText.headers.get("Title") + "{{page}}");
            addHeader(s, "Крыніца", subText.source);
            break;
        case kankardans: {
            subText.label = subText.authors != null ? String.join(",", subText.authors) : "———";
            subText.title = subText.headers.get("Title");
            addHeader(s, "Аўтар", parseThenJoinAuthors(subText.headers.get("Authors")));
            addHeader(s, "Назва", subText.headers.get("Title") + "{{page}}");
            addHeader(s, "Час стварэння", subText.headers.get("CreationYear"));
            addHeader(s, "Час публікацыі", subText.headers.get("PublicationYear"));
            break;
        }
        default:
            throw new RuntimeException("Unknown subcorpus: " + textInfo.subcorpus);
        }
        subText.passport = s.toString();
    }

    private String url(String url) {
        if (url == null) {
            return null;
        }
        return "<a href='" + url + "'>" + url + "</a>";
    }

    private void addHeader(StringBuilder s, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        s.append("<div><b>" + title + ":</b> " + value + "</div>");
    }

    enum SUBCORPUSES {
        skaryna, kankardans, teksty, pieraklady, sajty, wiki, nierazabranaje, dyjalektny
    };

    static final Collator BE = Collator.getInstance(new Locale("be"));

    Comparator<TextInfo> teksty = (o1, o2) -> {
        // першыя - найбольш раннія па даце стварэння альбо даце выдання. не пазначана
        // дата - на канец
        int c = Long.compare(earliestCreationPublication(o1, Long.MAX_VALUE), earliestCreationPublication(o2, Long.MAX_VALUE));
        if (c == 0) {
            c = compareAuthors(o1, o2);
        }
        if (c == 0) {
            c = BE.compare(o1.subtexts[0].headers.get("Title"), o2.subtexts[0].headers.get("Title"));
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
            c = BE.compare(o1.subtexts[0].headers.get("Title"), o2.subtexts[0].headers.get("Title"));
        }
        return c;
    };
    Comparator<TextInfo> wiki = (o1, o2) -> {
        int c = o1.subcorpus.compareTo(o2.subcorpus);
        if (c == 0) {
            c = BE.compare(o1.subtexts[0].headers.get("Title"), o2.subtexts[0].headers.get("Title"));
        }
        return c;
    };
    Comparator<TextInfo> dyjalektny = (o1, o2) -> {
        if (o1.subtexts[0].headers.get("Раён") == null) {
            throw new RuntimeException("Нявызначаны загаловак 'Раён' у " + o1.sourceFilePath);
        }
        if (o2.subtexts[0].headers.get("Раён") == null) {
            throw new RuntimeException("Нявызначаны загаловак 'Раён' у " + o2.sourceFilePath);
        }
        if (o1.subtexts[0].source == null) {
            throw new RuntimeException("Нявызначаны загаловак 'Крыніца' у " + o1.sourceFilePath);
        }
        if (o2.subtexts[0].source == null) {
            throw new RuntimeException("Нявызначаны загаловак 'Крыніца' у " + o2.sourceFilePath);
        }
        int c = BE.compare(o1.subtexts[0].headers.get("Раён"), o2.subtexts[0].headers.get("Раён"));
        if (c == 0) {
            c = BE.compare(o1.subtexts[0].source, o2.subtexts[0].source);
        }
        return c;
    };
    Comparator<TextInfo> skaryna = (o1, o2) -> {
        int c = BE.compare(o1.subtexts[0].headers.get("Title"), o2.subtexts[0].headers.get("Title"));
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

    static String parseThenJoinAuthors(String authors) {
        String[] list = AuthorsUtil.parseAuthors(authors);
        if (list == null) {
            return null;
        }
        return String.join(",", list);
    }

    static int compare(String s1, String s2) {
        if (s1 == null)
            s1 = "";
        if (s2 == null)
            s2 = "";
        return s1.compareTo(s2);
    }

    public static void main(String[] args) throws Exception {
        ProcessTexts.preprocessor = new ApplyAsnouny();
        PrepareCache3.main(args);
    }
}
