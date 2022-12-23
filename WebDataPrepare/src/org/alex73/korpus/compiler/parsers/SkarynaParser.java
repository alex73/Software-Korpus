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

public class SkarynaParser extends BaseParser {

    public SkarynaParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Consumer<MessageParsedText> publisher, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);
        TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly);
        MessageParsedText text = new MessageParsedText(1);
        text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
        text.textInfo.subcorpus = subcorpus;
        text.textInfo.subtexts[0].title = doc.headers.get("Title");
        text.textInfo.subtexts[0].textLabel = text.textInfo.subtexts[0].title;

        if (!headersOnly) {
            doc.parse(LanguageFactory.get(getLang(text.textInfo.subtexts[0].lang)), true, PrepareCache3.errors);
            text.paragraphs = get1LangParagraphs(new PtextToKorpus(doc.lines, true).paragraphs);
        }
        publisher.accept(text);
    }
}
