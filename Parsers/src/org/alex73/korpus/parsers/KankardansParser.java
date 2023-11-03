package org.alex73.korpus.parsers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;
import org.alex73.korpus.parsers.utils.TextFileHeaders;

public class KankardansParser {
    static final String PREFIX_TEXT = "№ тэксту ";
    static final String PREFIX_PAGE = "№ старонкі ";
    static final String PREFIX_ROW = "№ радка";

    static int fileIndex;
    static Output os;
    static String textTitle = "";
    static Ctf text = null;
    static Ctf.Page page = null;
    static Map<String, String> headers = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("KankardansParser <файл з канкардансам> <каталог каб захаваць падкорпус>");
            System.exit(1);
        }
        os = new Output(Path.of(args[1], "02.kankardans.zip"));
        parse(Path.of(args[0]));
        os.close();
    }

    static void parse(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);

        boolean inText = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(PREFIX_TEXT)) {
                flushText();
                inText = false;
                textTitle = line;
                text = new Ctf();
                text.languages = new Ctf.Language[1];
                text.languages[0] = new Ctf.Language();
                text.languages[0].lang = "bel";
                text.languages[0].pages = new Ctf.Page[0];
            } else if (line.startsWith(PREFIX_PAGE)) {
                if (!inText) {
                    inText = true;
                    // text.append("\n");
                }
                page = new Ctf.Page();
                page.paragraphs = new String[1];
                text.languages[0].pages = Arrays.copyOf(text.languages[0].pages, text.languages[0].pages.length + 1);
                text.languages[0].pages[text.languages[0].pages.length - 1] = page;
                page.pageNum = line.substring(PREFIX_PAGE.length()).trim();
            } else if (line.startsWith(PREFIX_ROW)) {
            } else if (line.startsWith("№")) {
                throw new Exception("Wrong format: " + line);
            } else if (line.startsWith("##")) {
                if (!inText) {
                    int p = line.indexOf(':');
                    headers.put(line.substring(2, p), line.substring(p + 1));
                } else {
                    throw new Exception("Wrong format: " + line);
                }
            } else {
                line = line.replaceAll("^[0-9]+", "").trim();
                if (page.paragraphs[0] == null) {
                    page.paragraphs[0] = line;
                } else {
                    page.paragraphs[0] += "\n" + line;
                }
            }
        }
        flushText();
    }

    static void flushText() throws Exception {
        if (text != null) {
            Ctf.Language la = text.languages[0];
            la.authors = Authors.autaryIndexes(headers.get("Authors"));
            la.creationTime = headers.get("CreationYear");
            la.publicationTime = headers.get("PublicationYear");

            la.label = la.authors != null ? String.join(",", la.authors) : "———";
            la.title = headers.get("Title") + "{{page}}";
            List<String> s = new ArrayList<>();
            TextFileHeaders.addHeader(s, "Аўтар", Authors.autaryPravapis(headers.get("Authors")));
            TextFileHeaders.addHeader(s, "Аўтар вядомы як", headers.get("Aka"));
            TextFileHeaders.addHeader(s, "Назва", headers.get("Title") + "{{page}}");
            TextFileHeaders.addHeader(s, "Час стварэння", headers.get("CreationYear"));
            TextFileHeaders.addHeader(s, "Час публікацыі", headers.get("PublicationYear"));
            la.headers = s.toArray(new String[0]);

            String file = textTitle.replaceAll("[^\\p{IsAlphabetic}\\p{Alnum}]", "_");
            if (file.length() > 120) {
                file = file.substring(0, 120);
            }
            os.write(file + ".ctf", text);
        }
        text = null;
        page = null;
        headers.clear();
    }
}
