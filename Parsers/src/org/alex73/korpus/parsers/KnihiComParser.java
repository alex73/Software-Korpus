package org.alex73.korpus.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.alex73.korpus.parsers.utils.HtmlKnihiComParser;
import org.alex73.korpus.parsers.utils.Output;
import org.alex73.korpus.parsers.utils.TextFileHeaders;
import org.alex73.korpus.utils.KorpusDateTime;

/**
 * Парсер для кніг з knihi.com.
 * 
 * Трэба выцягнуць усе старонкі сайту камандай:
 * 
 * "wget -m --no-parent https://knihi.com".
 */
public class KnihiComParser {

    static List<TF> teksty = new ArrayList<>();
    static List<TF> pieraklady = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("KnihiComParser <каталог з файламі knihi.com> <каталог з падкорпусамі>");
            System.exit(1);
        }

        Path in = Path.of(args[0]);
        List<Path> files = Files
                .find(in, Integer.MAX_VALUE, (p, a) -> a.isRegularFile() && p.toString().toLowerCase().endsWith(".html"), FileVisitOption.FOLLOW_LINKS).sorted()
                .toList();

        for (Path file : files) {
            String url = "https://knihi.com/" + in.relativize(file).toString();
            TF tf = parse(file, url);
            if (tf != null) {
                tf.file = in.relativize(file).toString().replaceAll("\\.html$", ".ctf");
                if (tf.pieraklad) {
                    pieraklady.add(tf);
                } else {
                    teksty.add(tf);
                }
            }
        }

        Collections.sort(teksty, tekstySort);
        Path fo1 = Path.of(args[1], "03.teksty.zip");
        try (Output os = new Output(fo1)) {
            for (TF tf : teksty) {
                os.write(tf.file, tf.text);
            }
        }
        Collections.sort(pieraklady, pierakladySort);
        Path fo2 = Path.of(args[1], "04.pieraklady.zip");
        try (Output os = new Output(fo2)) {
            for (TF tf : pieraklady) {
                os.write(tf.file, tf.text);
            }
        }
    }

    static class TF {
        String file;
        boolean pieraklad;
        Ctf text = new Ctf();
    }

    static TF parse(Path file, String url) throws Exception {
        System.out.println("Чытаем " + file + "...");

        byte[] data = Files.readAllBytes(file);
        HtmlKnihiComParser doc = new HtmlKnihiComParser(new ByteArrayInputStream(data));
        if (doc.textLines.isEmpty()) {
            return null;
        }

        String lang = doc.headers.get("Lang");
        String langOrig = doc.headers.get("LangOrig");
        if ("bel".equals(lang)) {
            lang = null;
        }
        if ("bel".equals(langOrig)) {
            langOrig = null;
        }
        if (lang != null) {
            // пераклад на іншую мову
            return null;
        }

        TF tf = new TF();
        if (langOrig != null) {
            tf.pieraklad = true;
        }

        Page p = new Page();
        p.paragraphs = doc.textLines.toArray(new String[0]);
        tf.text.setPages("bel", Arrays.asList(p));
        Ctf.Language la = tf.text.languages[0];

        String sa;
        if ((sa = doc.headers.get("StyleGenre")) != null) {
            tf.text.styleGenres = TextFileHeaders.splitAndTrim(sa);
        }

        la.authors = Authors.autaryIndexes(doc.headers.get(langOrig != null ? "Translation" : "Authors"));
        String authors = Authors.autaryPravapis(doc.headers.get(langOrig != null ? "Translation" : "Authors"));
        la.label = la.authors != null ? String.join(",", la.authors) : "———";
        la.title = (authors == null ? "" : (authors + ".")) + doc.headers.get("Title");
        List<String> s = new ArrayList<>();
        TextFileHeaders.addHeader(s, "Аўтары", Authors.autaryPravapis(doc.headers.get("Authors")));
        TextFileHeaders.addHeader(s, "Перакладчык", Authors.autaryPravapis(doc.headers.get("Translation")));
        TextFileHeaders.addHeader(s, "Пераклад з", doc.headers.get("LangOrig"));
        TextFileHeaders.addHeader(s, "Назва", doc.headers.get("Title"));
        TextFileHeaders.addHeader(s, "Стыль/жанр", doc.headers.get("StyleGenre"));
        TextFileHeaders.addHeader(s, "Выданне", doc.headers.get("Edition"));
        TextFileHeaders.addHeader(s, "Час стварэння", doc.headers.get("CreationYear"));
        TextFileHeaders.addHeader(s, "Час публікацыі", doc.headers.get("PublicationYear"));
        TextFileHeaders.addHeader(s, "URL", "<a href='" + url + "'>" + url + "</a>");
        la.headers = s.toArray(new String[0]);

        la.creationTime = doc.headers.get("CreationYear");
        la.publicationTime = doc.headers.get("PublicationYear");

        return tf;
    }

    static class Autary {
        public Map<String, String> author2pravapis;
        public Map<String, String> author2index;
    }

    static final Collator BE = Collator.getInstance(Locale.forLanguageTag("be"));
    static Comparator<TF> tekstySort = (o1, o2) -> {
        // першыя - найбольш раннія па даце стварэння альбо даце выдання. не пазначана
        // дата - на канец
        int c = Long.compare(earliestCreationPublication(o1.text, Long.MAX_VALUE), earliestCreationPublication(o2.text, Long.MAX_VALUE));
        if (c == 0) {
            c = compareAuthors(o1.text, o2.text);
        }
        if (c == 0) {
            c = BE.compare(o1.text.languages[0].title, o2.text.languages[0].title);
        }
        return c;
    };
    static Comparator<TF> pierakladySort = (o1, o2) -> {
        // першыя - найбольш раннія па даце выдання. не пазначана дата - на канец
        int c = Long.compare(earliestPublication(o1.text, Long.MAX_VALUE), earliestPublication(o2.text, Long.MAX_VALUE));
        if (c == 0) {
            c = compareAuthors(o1.text, o2.text);
        }
        if (c == 0) {
            c = BE.compare(o1.text.languages[0].title, o2.text.languages[0].title);
        }
        return c;
    };

    static long earliestCreationPublication(Ctf ti, long defaultValue) {
        Long earliest = timeEarliest(ti.languages[0].creationTime);
        if (earliest == null) {
            earliest = timeEarliest(ti.languages[0].publicationTime);
        }
        if (earliest == null) {
            earliest = defaultValue;
        }
        return earliest.longValue();
    }

    static long earliestPublication(Ctf ti, long defaultValue) {
        Long earliest = timeEarliest(ti.languages[0].publicationTime);
        if (earliest == null) {
            earliest = defaultValue;
        }
        return earliest.longValue();
    }

    static Long timeEarliest(String time) {
        if (time == null) {
            return null;
        }
        KorpusDateTime dt = new KorpusDateTime(time);
        return dt.earliest();
    }

    static int compareAuthors(Ctf o1, Ctf o2) {
        int ac1 = o1.languages[0].authors != null ? o1.languages[0].authors.length : 0;
        int ac2 = o2.languages[0].authors != null ? o2.languages[0].authors.length : 0;
        int c = 0;
        for (int i = 0; i < Math.max(ac1, ac2); i++) {
            String a1, a2;
            try {
                a1 = o1.languages[0].authors[i];
            } catch (Exception ex) {
                a1 = "";
            }
            try {
                a2 = o2.languages[0].authors[i];
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
}
