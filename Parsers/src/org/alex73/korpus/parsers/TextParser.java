package org.alex73.korpus.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.AuthorsUtil;
import org.alex73.korpus.parsers.utils.Output;
import org.alex73.korpus.parsers.utils.TextFileHeaders;
import org.alex73.korpus.parsers.utils.TextFileParser;

public class TextParser {
    static List<TF> texty = new ArrayList<>();
    static List<TF> piekaklady = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("TextParser <каталог з зыходнымі файламі> <файл каб захаваць падкорпус тэкстаў> <файл каб захаваць падкорпус перакладаў>");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        AuthorsUtil.init(in);

        List<Path> files = Files
                .find(in, Integer.MAX_VALUE, (p, a) -> a.isRegularFile() && p.toString().toLowerCase().endsWith(".text"), FileVisitOption.FOLLOW_LINKS).sorted()
                .toList();

        for (Path file : files) {
            TF tf = parse(file);
            if (tf != null) {
                tf.file = in.relativize(file).toString().replaceAll("\\.text$", ".ctf");
                if (tf.pieraklad) {
                    piekaklady.add(tf);
                } else {
                    texty.add(tf);
                }
            }
        }

//        Collections.sort(texts, (o1, o2) -> BE.compare(o1.text.title, o2.text.title));
        Path fo1 = Path.of(args[1]);
        try (Output os = new Output(fo1)) {
            for (TF tf : texty) {
                os.write(tf.file, tf.text);
            }
        }
        Path fo2 = Path.of(args[2]);
        try (Output os = new Output(fo2)) {
            for (TF tf : piekaklady) {
                os.write(tf.file, tf.text);
            }
        }
    }

    static class TF {
        boolean pieraklad;
        String file;
        Ctf text = new Ctf();
    }

    static TF parse(Path file) throws Exception {
        System.out.println("Чытаем " + file + "...");

        byte[] data = Files.readAllBytes(file);
        TextFileParser.OneText doc = new TextFileParser(new ByteArrayInputStream(data)).oneTextExpected();

        TF tf = new TF();

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
        tf.pieraklad = langOrig != null;

        tf.text.setParagraphs("bel", doc.paragraphs);

        String sa;
        tf.text.languages[0].authors = AuthorsUtil.reverseNames(AuthorsUtil.parseAuthors(doc.headers.get(tf.pieraklad ? "Translation" : "Authors")));
        if ((sa = doc.headers.get("StyleGenre")) != null) {
            tf.text.styleGenres = TextFileHeaders.splitAndTrim(sa);
        }

        tf.text.languages[0].label = tf.text.languages[0].authors != null ? String.join(",", tf.text.languages[0].authors) : "———";
        String textAuthors = TextFileHeaders.parseThenJoinAuthors(doc.headers.get("Authors"));
        tf.text.languages[0].title = (textAuthors == null ? "" : (textAuthors + ".")) + doc.headers.get("Title");
        List<String> s = new ArrayList<>();
        TextFileHeaders.addHeader(s, "URL", doc.headers.get("URL"));
        TextFileHeaders.addHeader(s, "Аўтары", textAuthors);
        TextFileHeaders.addHeader(s, "Перакладчык", TextFileHeaders.parseThenJoinAuthors(doc.headers.get("Translation")));
        TextFileHeaders.addHeader(s, "Пераклад з", doc.headers.get("LangOrig"));
        TextFileHeaders.addHeader(s, "Назва", doc.headers.get("Title"));
        TextFileHeaders.addHeader(s, "Стыль/жанр", doc.headers.get("StyleGenre"));
        TextFileHeaders.addHeader(s, "Выданне", doc.headers.get("Edition"));
        TextFileHeaders.addHeader(s, "Файл", doc.headers.get("File"));
        TextFileHeaders.addHeader(s, "Час стварэння", doc.headers.get("CreationYear"));
        TextFileHeaders.addHeader(s, "Час публікацыі", doc.headers.get("PublicationYear"));
        tf.text.languages[0].headers = s.toArray(new String[0]);

        tf.text.languages[0].creationTime = doc.headers.get("CreationYear");
        tf.text.languages[0].publicationTime = doc.headers.get("PublicationYear");

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
