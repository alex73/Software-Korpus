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

public class OcrTextParser extends BaseParser {
    public OcrTextParser(String subcorpus, Path file) {
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
            textInfo.url = doc.headers.get("URL");
            textInfo.file = doc.headers.get("File");
            textInfo.source = doc.headers.get("Source");
            textInfo.title = doc.headers.get("Title");
            textInfo.details = doc.headers.get("Details");
            textInfo.textLabel = textInfo.source;
            if (headersOnly) {
                ProcessHeaders.process(textInfo);
            } else {
                ProcessTexts.process(textInfo, new PtextToKorpus(doc.lines, false).paragraphs);
            }
        });
    }
}
