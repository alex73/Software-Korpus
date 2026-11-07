package org.alex73.korpus.parsers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.base.Ctf.Page;
import org.alex73.korpus.parsers.utils.Output;

import com.github.luben.zstd.ZstdInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Парсер для канвертавання тэкстаў у фармаце HPLT v3 (https://hplt-project.org/datasets/v3.0) у фармат CTF.
 * Чытае файлы *.jsonl.zst і стварае падкорпус у zip-архіве.
 */
public class HpltParser {

    private static final Pattern RE_DATE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern RE_YEAR = Pattern.compile("^(\\d{4})");
    private static final Pattern RE_SCORE = Pattern.compile("^(\\d+)_");

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private int articlesCount = 0;

    public static class HpltDoc {
        public String id;
        public String u;
        public String ts;
        public String text;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("HpltParser <каталог з файламі *.jsonl.zst ці асобны файл> <каталог каб захаваць падкорпус ці шлях да .zip>");
            System.exit(1);
        }

        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        if (Files.isDirectory(out) || !out.toString().toLowerCase().endsWith(".zip")) {
            out = out.resolve("09.hplt.zip");
        }

        HpltParser parser = new HpltParser();
        parser.run(in, out);
    }

    public void run(Path in, Path out) throws Exception {
        List<Path> files = collectFiles(in);
        if (files.isEmpty()) {
            System.out.println("Няма файлаў для апрацоўкі ў " + in.toAbsolutePath());
            return;
        }

        System.out.println("Знойдзена файлаў для апрацоўкі: " + files.size());
        Path parent = out.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (Output os = new Output(out)) {
            for (Path file : files) {
                parseFile(file, os);
            }
        }
        System.out.println("\nАпрацавана дакументаў агулам: " + articlesCount);
    }

    public List<Path> collectFiles(Path in) throws Exception {
        if (!Files.exists(in)) {
            return List.of();
        }
        if (Files.isRegularFile(in)) {
            return List.of(in);
        }

        try (Stream<Path> stream = Files.walk(in)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".zst"))
                    .sorted(Comparator.comparingInt(HpltParser::extractScore).reversed()
                            .thenComparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    static int extractScore(Path p) {
        Matcher m = RE_SCORE.matcher(p.getFileName().toString());
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public void parseFile(Path file, Output os) throws Exception {
        System.out.println("Чытаем " + file + "...");
        try (InputStream fis = Files.newInputStream(file);
             InputStream bis = new BufferedInputStream(fis, 1024 * 1024);
             InputStream zis = new ZstdInputStream(bis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8), 1024 * 1024)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                HpltDoc doc = gson.fromJson(line, HpltDoc.class);
                Ctf ctf = parseDoc(doc);
                if (ctf != null) {
                    articlesCount++;
                    if (articlesCount % 1000 == 0) {
                        System.out.print("    " + articlesCount + "\r");
                    }
                    String entryName = "hplt-" + new DecimalFormat("00000").format(articlesCount / 1000) + "/"
                            + new DecimalFormat("00000000").format(articlesCount) + ".ctf";
                    os.write(entryName, ctf);
                }
            }
        }
    }

    public Ctf parseDoc(HpltDoc doc) {
        if (doc == null || doc.text == null || doc.text.isBlank()) {
            return null;
        }

        List<String> paragraphs = extractParagraphs(doc.text);
        if (paragraphs.isEmpty()) {
            return null;
        }

        String title = determineTitle(paragraphs.get(0), doc.u, doc.id);
        String host = extractHost(doc.u);
        String pubDate = extractDate(doc.ts);

        Ctf result = new Ctf();
        Page p = new Page();
        p.paragraphs = paragraphs.toArray(new String[0]);
        result.setPages("bel", List.of(p));

        Ctf.Language la = result.languages[0];
        la.title = title;
        la.label = host;
        la.sourceName = host;
        if (pubDate != null) {
            la.publicationTime = pubDate;
        }

        List<String> headers = new ArrayList<>();
        headers.add("Крыніца:HPLT v3");
        if (doc.u != null && !doc.u.isBlank()) {
            headers.add("URL:" + doc.u);
        }
        headers.add("Назва:" + title);
        if (pubDate != null) {
            headers.add("Час публікацыі:" + pubDate);
        }
        if (doc.id != null && !doc.id.isBlank()) {
            headers.add("ID:" + doc.id);
        }
        la.headers = headers.toArray(new String[0]);

        return result;
    }

    public static List<String> extractParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        if (text == null) {
            return paragraphs;
        }
        for (String line : text.split("\n")) {
            String fixed = fixText(line);
            if (!fixed.isEmpty()) {
                paragraphs.add(fixed);
            }
        }
        return paragraphs;
    }

    public static String fixText(String text) {
        if (text == null) {
            return "";
        }
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        text = text.replace('\u00A0', ' ');
        return text.trim();
    }

    public static String determineTitle(String firstParagraph, String url, String id) {
        if (firstParagraph != null && !firstParagraph.isBlank()) {
            String title = firstParagraph.trim();
            if (title.length() > 120) {
                int lastSpace = title.lastIndexOf(' ', 120);
                if (lastSpace > 40) {
                    title = title.substring(0, lastSpace) + "...";
                } else {
                    title = title.substring(0, 120) + "...";
                }
            }
            return title;
        }
        if (url != null && !url.isBlank()) {
            return url;
        }
        if (id != null && !id.isBlank()) {
            return id;
        }
        return "Без назвы";
    }

    public static String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return "hplt";
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null) {
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception ignored) {
        }
        return "hplt";
    }

    public static String extractDate(String ts) {
        if (ts == null || ts.isBlank()) {
            return null;
        }
        Matcher mDate = RE_DATE.matcher(ts.trim());
        if (mDate.find()) {
            return mDate.group(1);
        }
        Matcher mYear = RE_YEAR.matcher(ts.trim());
        if (mYear.find()) {
            return mYear.group(1);
        }
        return null;
    }

    public int getArticlesCount() {
        return articlesCount;
    }
}
