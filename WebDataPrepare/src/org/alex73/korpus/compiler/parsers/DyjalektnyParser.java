package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.alex73.korpus.compiler.MessageParsedText;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.languages.LanguageFactory;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class DyjalektnyParser extends BaseParser {

    public DyjalektnyParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);
        MessageParsedText text = new MessageParsedText(1);
        TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly);
        text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
        text.textInfo.subcorpus = subcorpus;
        text.textInfo.subtexts[0].details = doc.headers.get("Place");
        text.textInfo.subtexts[0].source = doc.headers.get("Infarmant");
        String s;
        if ((s = doc.headers.get("Zbiralniki")) != null) {
            text.textInfo.subtexts[0].authors = splitAndTrim(s);
        }
        text.textInfo.subtexts[0].creationTime = getAndCheckYears(doc.headers.get("RecordYear"));
        text.textInfo.subtexts[0].textLabel = text.textInfo.subtexts[0].details;

        if (!headersOnly) {
            doc.parse(LanguageFactory.get(getLang(text.textInfo.subtexts[0].lang)), true, PrepareCache3.errors);
            text.paragraphs[0] = new PtextToKorpus(doc.lines, true).getParagraphs();
        }
        publisher.accept(text);
    }
}
