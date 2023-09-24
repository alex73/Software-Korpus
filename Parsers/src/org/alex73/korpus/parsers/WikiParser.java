package org.alex73.korpus.parsers;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.alex73.korpus.base.Ctf;
import org.alex73.korpus.parsers.utils.Output;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Чытае дамп wiki з
 * https://dumps.wikimedia.org/bewiki/latest/bewiki-latest-pages-articles.xml.bz2
 * і
 * https://dumps.wikimedia.org/be_x_oldwiki/latest/be_x_oldwiki-latest-pages-articles.xml.bz2
 */
public class WikiParser {
    // Выкідаем: назва старонкі пачынаецца з 'Катэгорыя:', 'Файл:', 'MediaWiki:',
    // 'Шаблон:'
    static final String[] SKIP_TITLE_MARKERS = new String[] { "Катэгорыя:", "Файл:", "MediaWiki:", "Шаблон:", "Шаблён:", "Вікіпедыя:", "Вікіпэдыя:" };
    // Выкідаем: тэкст пачынаецца з #REDIRECT
    static final String[] SKIP_TEXT_MARKERS = new String[] { "#REDIRECT", "#перанакіраваньне" };
    static final Pattern UNICODE_CHARS = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

    static String urlPrefix;
    static String source;
    static Output os;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("WikiParser <каталог з зыходнымі файламі> <файл каб захаваць падкорпус>");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        os = new Output(out);
        parse(in.resolve("bewiki-latest-pages-articles.xml.bz2"));
        parse(in.resolve("be_x_oldwiki-latest-pages-articles.xml.bz2"));
        os.close();
    }

    static void parse(Path in) throws Exception {
        if (!Files.exists(in)) {
            System.out.println("Няма файла " + in.toAbsolutePath());
            System.exit(1);
        }

        String fn = in.getFileName().toString();
        int end = fn.indexOf("wiki");
        String lang = fn.substring(0, end);
        urlPrefix = "https://" + lang + ".wikipedia.org/wiki/";
        source = "wiki:" + lang;

        int articlesCount = 0;

        try (InputStream ins = new BZip2CompressorInputStream(new BufferedInputStream(Files.newInputStream(in)))) {
            System.out.println("Чытаем " + in + "...");
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(ins);
            String pageTitle = null;
            StringBuilder str = new StringBuilder();
            while (reader.hasNext()) {
                int eventType = reader.next();
                switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    str.setLength(0);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    String elementName = reader.getLocalName();
                    switch (elementName) {
                    case "title":
                        pageTitle = str.toString();
                        break;
                    case "text":
                        Ctf text = parseArticle(pageTitle, str.toString());
                        if (text != null) {
                            articlesCount++;
                            if (articlesCount % 1000 == 0) {
                                System.out.print("    " + articlesCount + "\r");
                            }
                            String entryName = lang + "-" + new DecimalFormat("00000").format(articlesCount / 1000) + "/"
                                    + new DecimalFormat("00000000").format(articlesCount) + ".ctf";
                            os.write(entryName, text);
                        }
                        break;
                    }
                    break;
                case XMLStreamReader.CHARACTERS:
                case XMLStreamReader.CDATA:
                    str.append(reader.getText());
                    break;
                }
            }
            reader.close();
        }
    }

    static Ctf parseArticle(String inTitle, String inText) {
        String title = inTitle.trim();
        String text = inText.trim();
        for (String st : SKIP_TITLE_MARKERS) {
            if (title.startsWith(st)) {
                return null;
            }
        }
        for (String st : SKIP_TEXT_MARKERS) {
            if (text.startsWith(st)) {
                return null;
            }
        }

        text = fixText(text);
        List<String> paragraphs = new ArrayList<>();
        StringBuilder ptext = new StringBuilder();
        for (String s : text.split("\n")) {
            s = s.trim();
            if (s.isEmpty()) {
                if (ptext.length() > 0) {
                    if (!ptext.toString().replace('\n', ' ').trim().isEmpty()) {
                        paragraphs.add(ptext.toString().trim());
                    }
                    ptext.setLength(0);
                }
            } else {
                ptext.append(s).append('\n');
            }
        }
        if (!ptext.toString().replace('\n', ' ').trim().isEmpty()) {
            paragraphs.add(ptext.toString().trim());
        }

        Ctf result = new Ctf();
        result.setParagraphs("bel", paragraphs);
        result.languages[0].label = source;
        result.languages[0].title = title;
        result.languages[0].headers = new String[] { "Крыніца:" + source, "URL:" + urlPrefix + title, "Назва:" + title };
        return result;
    }

    static String fixText(String text) {
        // remove links but leave labels
        text = text.replaceAll("\\[\\[[^\\]]*\\|([^\\]]*)\\]\\]", "$1");
        text = text.replaceAll("\\[\\[[^\\]]*:[^\\]]*\\]\\]", "");
        text = text.replaceAll("\\[\\[([^\\]]*)\\]\\]", "$1");
        text = text.replaceAll("\\[\\s*[^\\]\\s]*\\s+([^\\]]*)\\]", "$1");
        text = text.replaceAll("\\[[^\\]]*\\]", "");
        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return text;
    }
}
