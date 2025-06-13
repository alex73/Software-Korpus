package org.alex73.korpus.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;
import org.alex73.korpus.parsers.utils.TextFileHeaders;
import org.alex73.korpus.parsers.utils.TextFileParser;

/**
 * Разбірае файлы фармату .text.
 */
public class TextFormatParser {

    protected List<TF> texty = new ArrayList<>();

    public void run(Path in, Path out) throws Exception {
        List<Path> files = Files
                .find(in, Integer.MAX_VALUE, (p, a) -> a.isRegularFile() && p.toString().toLowerCase().endsWith(".text"), FileVisitOption.FOLLOW_LINKS).sorted()
                .toList();

        for (Path file : files) {
            TF tf = parse(in, file);
            if (tf != null && allow(tf)) {
                texty.add(tf);
            }
        }

        sort();
//        Collections.sort(texts, (o1, o2) -> BE.compare(o1.text.title, o2.text.title));
        try (Output os = new Output(out)) {
            for (TF tf : texty) {
                os.write(tf.file, tf.text);
            }
        }
    }

    protected boolean allow(TF book) {
        return true;
    }

    protected void sort() {
    }

    public static class TF {
        public String file;
        public Ctf text = new Ctf();
    }

    static TF parse(Path in, Path file) throws Exception {
        System.out.println("Чытаем " + file + "...");

        byte[] data = Files.readAllBytes(file);
        TextFileParser.OneText doc = new TextFileParser(new ByteArrayInputStream(data)).oneTextExpected();

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
        tf.file = in.relativize(file).toString().replaceAll("\\.text$", ".ctf");
        tf.text.setPages("bel", doc.pages);
        Ctf.Language la = tf.text.languages[0];

        String sa;
        la.authors = Authors.autaryIndexes(doc.headers.get(langOrig != null ? "Translation" : "Authors"));
        if ((sa = doc.headers.get("StyleGenre")) != null) {
            tf.text.styleGenres = TextFileHeaders.splitAndTrim(sa);
        }

        la.label = la.authors != null ? String.join(",", la.authors) : "———";
        String textAuthors = Authors.autaryPravapis(doc.headers.get("Authors"));
        la.title = (textAuthors == null ? "" : (textAuthors + ". ")) + doc.headers.get("Title");
        List<String> s = new ArrayList<>();
        TextFileHeaders.addHeader(s, "Аўтары", textAuthors);
        TextFileHeaders.addHeader(s, "Перакладчык", Authors.autaryPravapis(doc.headers.get("Translation")));
        TextFileHeaders.addHeader(s, "Пераклад з", doc.headers.get("LangOrig"));
        TextFileHeaders.addHeader(s, "Назва", doc.headers.get("Title"));
        TextFileHeaders.addHeader(s, "Стыль/жанр", doc.headers.get("StyleGenre"));
        TextFileHeaders.addHeader(s, "Выданне", doc.headers.get("Edition"));
        TextFileHeaders.addHeader(s, "Час стварэння", doc.headers.get("CreationYear"));
        TextFileHeaders.addHeader(s, "Час публікацыі", doc.headers.get("PublicationYear"));
        la.headers = s.toArray(new String[0]);

        la.creationTime = doc.headers.get("CreationYear");
        la.publicationTime = doc.headers.get("PublicationYear");

        return tf;
    }

    /*
     * static Comparator<TF> teksty = (o1, o2) -> { // першыя - найбольш раннія па
     * даце стварэння альбо даце выдання. не пазначана // дата - на канец int c =
     * Long.compare(earliestCreationPublication(o1, Long.MAX_VALUE),
     * earliestCreationPublication(o2, Long.MAX_VALUE)); if (c == 0) { c =
     * compareAuthors(o1, o2); } if (c == 0) { c =
     * BE.compare(o1.subtexts[0].headers.get("Title"),
     * o2.subtexts[0].headers.get("Title")); } return c; }; static Comparator<TF>
     * pieraklady = (o1, o2) -> { // першыя - найбольш раннія па даце выдання. не
     * пазначана дата - на канец int c = Long.compare(earliestPublication(o1,
     * Long.MAX_VALUE), earliestPublication(o2, Long.MAX_VALUE)); if (c == 0) { c =
     * compareAuthors(o1, o2); } if (c == 0) { c =
     * BE.compare(o1.subtexts[0].headers.get("Title"),
     * o2.subtexts[0].headers.get("Title")); } return c; };
     */
}
