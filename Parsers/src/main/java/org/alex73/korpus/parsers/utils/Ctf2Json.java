package org.alex73.korpus.parsers.utils;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.alex73.korpus.base.Ctf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Converts .ctf files into .json. Useful for export corpus texts for external applications.
 */
public class Ctf2Json {
    static final int BLOCK_SIZE_LINES = 100000;
    static final List<String> headers = new ArrayList<String>();
    static final List<String> lines = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        for (Path f : Files.list(Path.of(".")).sorted().toList()) {
            if (!f.getFileName().toString().endsWith(".zip")) {
                continue;
            }
            System.out.println("Read " + f);
            try (Writer wr = Files.newBufferedWriter(Path.of(f.toString() + ".paragraphs.json"))) {
                try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(f))) {
                    ZipEntry zipEntry = null;
                    while ((zipEntry = zip.getNextEntry()) != null) {
                        if (!zipEntry.getName().endsWith(".ctf")) {
                            throw new Exception("Wrong file name: " + f + "!" + zipEntry.getName());
                        }
                        process(zip.readAllBytes());
                        if (lines.size() > BLOCK_SIZE_LINES) {
                            flush(wr);
                        }
                    }
                    flush(wr);
                }
            }
            try (Writer wr = Files.newBufferedWriter(Path.of(f.toString() + ".texts.json"))) {
                for (String s : headers) {
                    wr.write(s);
                    wr.write('\n');
                }
                headers.clear();
            }
        }
    }

    static void process(byte[] file) throws Exception {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        Ctf data = gson.fromJson(new String(file, StandardCharsets.UTF_8), Ctf.class);

        for (Ctf.Language lang : data.languages) {
            if (lang.lang.equals("bel")) {
                TextInfo ti = new TextInfo();
                ti.docId = headers.size();
                ti.styleGenres = data.styleGenres;
                ti.creationTime = lang.creationTime;
                ti.publicationTime = lang.publicationTime;
                headers.add(gson.toJson(ti));
                List<String> addLines = List.of(lang.pages).parallelStream().flatMap(p -> List.of(p.paragraphs).stream()).parallel().map(p -> {
                    ParagraphInfo pi = new ParagraphInfo();
                    pi.docId = ti.docId;
                    pi.paragraph = p;
                    String r = gson.toJson(pi);
                    if (r.indexOf('\n') >= 0) {
                        throw new RuntimeException(r);
                    }
                    return r;
                }).toList();
                lines.addAll(addLines);
            }
        }
    }

    static void flush(Writer wr) throws Exception {
        Collections.shuffle(lines);
        for (String s : lines) {
            wr.write(s);
            wr.write('\n');
        }
        lines.clear();
    }

    public static class TextInfo {
        int docId;
        String[] styleGenres;
        public String creationTime, publicationTime;
    }

    public static class ParagraphInfo {
        int docId;
        String paragraph;
    }
}
