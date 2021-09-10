package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.TextUtils;
import org.alex73.korpus.text.parser.PtextToKorpus;
import org.alex73.korpus.text.parser.TextFileParser;

public class OcrTextParser extends BaseParser {
    public OcrTextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue, boolean headersOnly) throws Exception {
        byte[] data = Files.readAllBytes(file);

        queue.execute(() -> {
            try {
                TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly,
                        PrepareCache3.errors);
                TextInfo textInfo = new TextInfo();
                textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
                textInfo.subcorpus = subcorpus;
                textInfo.url = "https://kamunikat.org/?pubid="
                        + file.getFileName().toString().replaceAll("^([0-9]+).+?$", "$1");
                textInfo.source = "kamunikat.org";
                TextUtils.fillFromHeaders(textInfo, doc.headers);
                PrepareCache3.process(textInfo, new PtextToKorpus(doc.lines, false).paragraphs);
            } catch (Exception ex) {
                PrepareCache3.errors.reportError("Error parse " + file, ex);
            }
        });
    }
}
