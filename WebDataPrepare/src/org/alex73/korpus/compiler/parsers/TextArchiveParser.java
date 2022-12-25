package org.alex73.korpus.compiler.parsers;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.Headers;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class TextArchiveParser extends BaseParser {
    public TextArchiveParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        Path headersFile = Paths.get(file.toString() + ".headers");
        Headers commonHeaders;
        if (Files.exists(headersFile)) {
            try (InputStream in = Files.newInputStream(headersFile)) {
                TextFileParser fp = new TextFileParser(in, true);
                commonHeaders = fp.headers;
            }
        } else {
            commonHeaders = new Headers();
        }

        try (ZipFile zip = new ZipFile(file.toFile())) {
            for (Enumeration<? extends ZipEntry> it = zip.entries(); it.hasMoreElements();) {
                ZipEntry en = it.nextElement();
                if (en.isDirectory()) {
                    continue;
                }
                TextFileParser doc;
                try (InputStream in = new BufferedInputStream(zip.getInputStream(en))) {
                    doc = new TextFileParser(in, headersOnly);
                }
                MessageParsedText text = new MessageParsedText(1);
                text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + "!" + en.getName();
                text.textInfo.subcorpus = subcorpus;
                text.textInfo.subtexts[0].source = commonHeaders.get("Source");
                text.textInfo.subtexts[0].title = doc.headers.get("Title");
                text.textInfo.subtexts[0].url = doc.headers.get("URL");
                text.textInfo.subtexts[0].publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));
                text.textInfo.subtexts[0].textLabel = text.textInfo.subtexts[0].source;
                String s;
                if ((s = doc.headers.get("StyleGenre")) != null) {
                    text.textInfo.styleGenres = splitAndTrim(s);
                }
                if (text.textInfo.subtexts[0].title == null) {
                    text.textInfo.subtexts[0].title = "";
                }
                if (!headersOnly) {
                    doc.parse(LanguageFactory.get(getLang(text.textInfo.subtexts[0].lang)), true, PrepareCache3.errors);
                    text.paragraphs = get1LangParagraphs(new PtextToKorpus(doc.lines, true).paragraphs);
                }
                publisher.accept(text);
            }
        }
    }
}
