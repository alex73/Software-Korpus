package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

/**
 * Texts will be moved to separate subcorpus "pieraklady" except in case of
 * System.getProperty("SUBCORPUS_PIERAKLADY") == "FALSE"
 */
public class TextParser extends BaseParser {
    private boolean pierakladyEnabled;

    public TextParser(String subcorpus, Path file) {
        super(subcorpus, file);
        pierakladyEnabled = !"FALSE".equals(System.getProperty("SUBCORPUS_PIERAKLADY"));
    }

    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);
        MessageParsedText text = new MessageParsedText();
        TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly);
        text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
        text.textInfo.subcorpus = subcorpus;
        text.textInfo.title = doc.headers.get("Title");
        text.textInfo.lang = doc.headers.get("Lang");
        if ("bel".equals(text.textInfo.lang)) {
            text.textInfo.lang = null;
        }
        text.textInfo.langOrig = doc.headers.get("LangOrig");
        if ("bel".equals(text.textInfo.langOrig)) {
            text.textInfo.langOrig = null;
        }
        if (text.textInfo.lang != null) {
            // пераклад на іншую мову
            return;
        }
        if (pierakladyEnabled && text.textInfo.langOrig != null && "teksty".equals(subcorpus)) {
            // корпус перакладаў
            text.textInfo.subcorpus = "pieraklady";
        }
        String s;
        if ((s = doc.headers.get("Authors")) != null) {
            text.textInfo.authors = splitAndTrim(s);
        }
        if ((s = doc.headers.get("StyleGenre")) != null) {
            text.textInfo.styleGenres = splitAndTrim(s);
        }
        text.textInfo.edition = doc.headers.get("Edition");
        text.textInfo.creationTime = getAndCheckYears(doc.headers.get("CreationYear"));
        text.textInfo.publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));
        text.textInfo.textLabel = text.textInfo.authors != null ? String.join(",", Arrays.asList(text.textInfo.authors)) : "———";

        AuthorsUtil.fixAuthors(text.textInfo);
        if (!headersOnly) {
            doc.parse(true, PrepareCache3.errors);
            text.paragraphs = new PtextToKorpus(doc.lines, true).paragraphs;
        }
        publisher.accept(text);
    }
}
