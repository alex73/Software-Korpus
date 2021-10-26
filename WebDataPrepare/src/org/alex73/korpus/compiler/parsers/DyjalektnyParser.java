package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.BaseParallelProcessor;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.ProcessHeaders;
import org.alex73.korpus.compiler.ProcessTexts;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class DyjalektnyParser extends BaseParser {

    public DyjalektnyParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(BaseParallelProcessor queue, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);
        queue.run(() -> {
            TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly, PrepareCache3.errors);
            TextInfo textInfo = new TextInfo();
            textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
            textInfo.subcorpus = subcorpus;
            textInfo.details = doc.headers.get("Place");
            textInfo.source = doc.headers.get("Infarmant");
            String s;
            if ((s = doc.headers.get("Zbiralniki")) != null) {
                textInfo.authors = splitAndTrim(s);
            }
            textInfo.creationTime = getAndCheckYears(doc.headers.get("RecordYear"));
            textInfo.textLabel = textInfo.details;

            if (headersOnly) {
                ProcessHeaders.process(textInfo);
            } else {
                ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, true).paragraphs);
            }
        });
    }
}
