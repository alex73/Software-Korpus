package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class TextParser extends BaseParser {

    public TextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);
        MessageParsedText text = new MessageParsedText(1);
        TextFileParser.OneText doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly).oneTextExpected();
        text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
        text.textInfo.subcorpus = subcorpus;

        text.textInfo.subtexts[0].headers = doc.headers.getAll();

        text.textInfo.subtexts[0].lang = doc.headers.get("Lang");
        if ("bel".equals(text.textInfo.subtexts[0].lang)) {
            text.textInfo.subtexts[0].lang = null;
        }
        text.textInfo.subtexts[0].langOrig = doc.headers.get("LangOrig");
        if ("bel".equals(text.textInfo.subtexts[0].langOrig)) {
            text.textInfo.subtexts[0].langOrig = null;
        }
        if (text.textInfo.subtexts[0].lang != null) {
            // пераклад на іншую мову
            return;
        }
        String s;
        boolean tr = text.textInfo.subtexts[0].langOrig != null; // пераклад
        text.textInfo.subtexts[0].authors = AuthorsUtil.reverseNames(AuthorsUtil.parseAuthors(doc.headers.get(tr ? "Translation" : "Authors")));
        if ((s = doc.headers.get("StyleGenre")) != null) {
            text.textInfo.styleGenres = splitAndTrim(s);
        }

        text.textInfo.subtexts[0].creationTime = getAndCheckYears(doc.headers.get("CreationYear"));
        text.textInfo.subtexts[0].publicationTime = getAndCheckYears(doc.headers.get("PublicationYear"));

        ProcessTexts.preprocessor.constructTextPassport(text.textInfo, text.textInfo.subtexts[0]);
        if (!headersOnly) {
            doc.parse(LanguageFactory.get(getLang(text.textInfo.subtexts[0].lang)), true, PrepareCache3.errors);
            text.paragraphs = get1LangParagraphs(new PtextToKorpus(doc.lines, true).paragraphs);
        }
        publisher.accept(text);
    }
}
