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
        try {
            TextFileParser allDocs = new TextFileParser(new ByteArrayInputStream(data), headersOnly);
            int textIndex = 0;
            for (TextFileParser.OneText doc : allDocs.texts) {
                textIndex++;
                MessageParsedText text = new MessageParsedText(1);
                text.textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString() + '_' + textIndex;
                text.textInfo.subcorpus = subcorpus;
                text.textInfo.subtexts[0].details = doc.headers.get("Раён");
                text.textInfo.subtexts[0].source = doc.headers.get("Інфармант");
                String s;
                if ((s = doc.headers.get("Збіральнік")) != null) {
                    text.textInfo.subtexts[0].authors = splitAndTrim(s);
                }
                text.textInfo.subtexts[0].creationTime = getAndCheckYears(doc.headers.get("Год запісу"));
                text.textInfo.subtexts[0].textLabel = text.textInfo.subtexts[0].details;

                if (!headersOnly) {
                    doc.parse(LanguageFactory.get(getLang(text.textInfo.subtexts[0].lang)), true, PrepareCache3.errors);
                    text.paragraphs = get1LangParagraphs(new PtextToKorpus(doc.lines, true).paragraphs);
                }
                publisher.accept(text);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error in " + file, ex);
        }
    }
}
