package org.alex73.korpus.compiler.parsers;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import org.alex73.korpus.base.TextInfo;
import org.alex73.korpus.compiler.PrepareCache3;
import org.alex73.korpus.compiler.TextUtils;
import org.alex73.korpus.text.parser.TextFileParser;

public class TextParser extends BaseParser {
    public TextParser(String subcorpus, Path file) {
        super(subcorpus, file);
    }

    @Override
    public void parse(Executor queue, boolean headersOnly) throws Exception {
        System.out.println(file);
        byte[] data = Files.readAllBytes(file);
        queue.execute(() -> {
            try {
                TextFileParser doc = new TextFileParser(new ByteArrayInputStream(data), headersOnly,
                        PrepareCache3.errors);
                TextInfo textInfo = new TextInfo();
                textInfo.sourceFilePath = PrepareCache3.INPUT.relativize(file).toString();
                textInfo.subcorpus = subcorpus;
                TextUtils.fillFromHeaders(textInfo, doc.headers);
                if ("bel".equals(textInfo.lang)) {
                    textInfo.lang = null;
                }
                if ("bel".equals(textInfo.langOrig)) {
                    textInfo.langOrig = null;
                }
                if (textInfo.lang != null) {
                    // пераклад на іншую мову
                    return;
                }
                if (textInfo.langOrig != null && "teksty".equals(subcorpus)) {
                    // корпус перакладаў
                    textInfo.subcorpus = "pieraklady";
                }
                PrepareCache3.process(textInfo, doc.paragraphs);
            } catch (Exception ex) {
                PrepareCache3.errors.reportError("Error parse " + file, ex);
            }
        });
    }
}
